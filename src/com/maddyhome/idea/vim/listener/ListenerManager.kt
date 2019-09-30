/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.VimTypedActionHandler
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.group.*
import com.maddyhome.idea.vim.group.visual.VimVisualTimer
import com.maddyhome.idea.vim.group.visual.moveCaretOneCharLeftFromSelectionEnd
import com.maddyhome.idea.vim.group.visual.vimSetSystemSelectionSilently
import com.maddyhome.idea.vim.helper.*
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.ui.ExEntryPanel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.Closeable

/**
 * @author Alex Plate
 */

/**
 * Base class for listener suppressors.
 * Children of this class have an ability to suppress editor listeners
 *
 * E.g.
 * ```
 *  CaretVimListenerSuppressor.lock()
 *  caret.moveToOffset(10) // vim's caret listener will not be executed
 *  CaretVimListenerSuppressor.unlock()
 * ````
 *
 *  Locks can be nested:
 * ```
 *      CaretVimListenerSuppressor.lock()
 *      moveCaret(caret) // vim's caret listener will not be executed
 *      CaretVimListenerSuppressor.unlock()
 *
 *  fun moveCaret(caret: Caret) {
 *      CaretVimListenerSuppressor.lock()
 *      caret.moveToOffset(10)
 *      CaretVimListenerSuppressor.unlock()
 *  }
 * ```
 *
 * [Locked] implements [Closeable], so you can use try-with-resources block
 *
 * java
 * ```
 * try (VimListenerSuppressor.Locked ignored = SelectionVimListenerSuppressor.INSTANCE.lock()) {
 *     ....
 * }
 * ```
 *
 * Kotlin
 * ```
 * SelectionVimListenerSuppressor.lock().use { ... }
 * ```
 */
sealed class VimListenerSuppressor{
  private var caretListenerSuppressor = 0

  fun lock(): Locked {
    caretListenerSuppressor++
    return Locked()
  }

  fun unlock(action: (() -> Unit)? = null) {
    if (action != null) {
      try {
        action()
      } finally {
          caretListenerSuppressor--
      }
    } else {
      caretListenerSuppressor--
    }
  }

  val isNotLocked: Boolean
    get() = caretListenerSuppressor == 0

  inner class Locked : Closeable  {
    override fun close() = unlock()
  }
}

object SelectionVimListenerSuppressor : VimListenerSuppressor()

object VimListenerManager {

  val logger = Logger.getInstance(VimListenerManager::class.java)

  fun turnOn() {
    GlobalListeners.enable()
    ProjectListeners.addAll()
    EditorListeners.addAll()
  }

  fun turnOff() {
    GlobalListeners.disable()
    ProjectListeners.removeAll()
    EditorListeners.removeAll()
  }

  object GlobalListeners {
    @JvmStatic
    fun enable() {
      val typedAction = EditorActionManager.getInstance().typedAction
      if (typedAction.rawHandler !is VimTypedActionHandler) {
        // Actually this if should always be true, but just as protection
        EventFacade.getInstance().setupTypedActionHandler(VimTypedActionHandler(typedAction.rawHandler))
      }

      OptionsManager.number.addOptionChangeListener(EditorGroup.NumberChangeListener.INSTANCE)
      OptionsManager.relativenumber.addOptionChangeListener(EditorGroup.NumberChangeListener.INSTANCE)

      EventFacade.getInstance().addEditorFactoryListener(VimEditorFactoryListener, ApplicationManager.getApplication())
    }

    fun disable() {
      EventFacade.getInstance().restoreTypedActionHandler()

      OptionsManager.number.removeOptionChangeListener(EditorGroup.NumberChangeListener.INSTANCE)
      OptionsManager.relativenumber.removeOptionChangeListener(EditorGroup.NumberChangeListener.INSTANCE)

      EventFacade.getInstance().removeEditorFactoryListener(VimEditorFactoryListener)
    }
  }

  object ProjectListeners {
    fun add(project: Project) {
      val eventFacade = EventFacade.getInstance()
      eventFacade.connectBookmarkListener(project, MarkGroup.MarkListener())
      eventFacade.connectFileEditorManagerListener(project, VimFileEditorManagerListener)
      IdeaSpecifics.addIdeaSpecificsListeners(project)
    }

    fun removeAll() {
      // Project listeners are self-disposable, so there is no need to unregister them on project close
      EventFacade.getInstance().disableBusConnection()
      ProjectManager.getInstance().openProjects.filterNot { it.isDisposed }.forEach { IdeaSpecifics.removeIdeaSpecificsListeners(it) }
    }

    fun addAll() {
      ProjectManager.getInstance().openProjects.filterNot { it.isDisposed }.forEach { add(it) }
    }
  }

  object EditorListeners {
    fun addAll() {
      val editors = EditorFactory.getInstance().allEditors
      for (editor in editors) {
        if (!editor.vimMotionGroup) {
          add(editor)
          editor.vimMotionGroup = true
        }
      }
    }

    fun removeAll() {
      val editors = EditorFactory.getInstance().allEditors
      for (editor in editors) {
        if (editor.vimMotionGroup) {
          remove(editor)
          editor.vimMotionGroup = false
        }
      }
    }

    @JvmStatic
    fun add(editor: Editor) {
      val eventFacade = EventFacade.getInstance()
      eventFacade.addEditorMouseListener(editor, EditorMouseHandler)
      eventFacade.addEditorMouseMotionListener(editor, EditorMouseHandler)
      eventFacade.addEditorSelectionListener(editor, EditorSelectionHandler)
      eventFacade.addComponentMouseListener(editor.contentComponent, ComponentMouseListener)
    }

    @JvmStatic
    fun remove(editor: Editor) {
      val eventFacade = EventFacade.getInstance()
      eventFacade.removeEditorMouseListener(editor, EditorMouseHandler)
      eventFacade.removeEditorMouseMotionListener(editor, EditorMouseHandler)
      eventFacade.removeEditorSelectionListener(editor, EditorSelectionHandler)
      eventFacade.removeComponentMouseListener(editor.contentComponent, ComponentMouseListener)
    }
  }

  object VimFileEditorManagerListener : FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {
      MotionGroup.fileEditorManagerSelectionChangedCallback(event)
      FileGroup.fileEditorManagerSelectionChangedCallback(event)
      SearchGroup.fileEditorManagerSelectionChangedCallback(event)
    }
  }

  private object VimEditorFactoryListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
      VimPlugin.getEditor().editorCreated(event)
      VimPlugin.getMotion().editorCreated(event)
      VimPlugin.getChange().editorCreated(event)
      VimPlugin.statisticReport()
    }

    override fun editorReleased(event: EditorFactoryEvent) {
      VimPlugin.getEditor().editorReleased(event)
      VimPlugin.getMotion().editorReleased(event)
      VimPlugin.getChange().editorReleased(event)
      VimPlugin.getMark().editorReleased(event)
    }
  }

  private object EditorSelectionHandler : SelectionListener {
    private var myMakingChanges = false

    /**
     * This event is executed for each caret using [com.intellij.openapi.editor.CaretModel.runForEachCaret]
     */
    override fun selectionChanged(selectionEvent: SelectionEvent) {
      val editor = selectionEvent.editor
      val document = editor.document

      if (SelectionVimListenerSuppressor.isNotLocked) {
        logger.info("Adjust non vim selection change")
        VimPlugin.getVisualMotion().controlNonVimSelectionChange(editor)
      }

      if (myMakingChanges || document is DocumentEx && document.isInEventsHandling) {
        return
      }

      myMakingChanges = true
      try {
        // Synchronize selections between editors
        val newRange = selectionEvent.newRange
        for (e in EditorFactory.getInstance().getEditors(document)) {
          if (e != editor) {
            e.selectionModel
              .vimSetSystemSelectionSilently(newRange.startOffset, newRange.endOffset)
          }
        }
      } finally {
        myMakingChanges = false
      }
    }
  }

  private object EditorMouseHandler : EditorMouseListener, EditorMouseMotionListener {
    private var mouseDragging = false

    override fun mouseDragged(e: EditorMouseEvent) {
      if (!mouseDragging) {
        logger.debug("Mouse dragging")
        SelectionVimListenerSuppressor.lock()
        VimVisualTimer.swingTimer?.stop()
        mouseDragging = true
        val caret = e.editor.caretModel.primaryCaret
        if (onLineEnd(caret)) {
          // UX protection for case when user performs a small dragging while putting caret on line end
          caret.removeSelection()
          ChangeGroup.resetCaret(e.editor, true)
        }
      }
      if (mouseDragging && e.editor.caretModel.primaryCaret.hasSelection()) {
        ChangeGroup.resetCaret(e.editor, true)
      }
    }

    private fun onLineEnd(caret: Caret): Boolean {
      val editor = caret.editor
      val lineEnd = EditorHelper.getLineEndForOffset(editor, caret.offset)
      val lineStart = EditorHelper.getLineStartForOffset(editor, caret.offset)
      return caret.offset == lineEnd && lineEnd != lineStart && caret.offset - 1 == caret.selectionStart && caret.offset == caret.selectionEnd
    }

    override fun mouseReleased(event: EditorMouseEvent) {
      if (mouseDragging) {
        logger.debug("Release mouse after dragging")
        val editor = event.editor
        val caret = editor.caretModel.primaryCaret
        SelectionVimListenerSuppressor.unlock {
          val predictedMode = VimPlugin.getVisualMotion().predictMode(editor, VimListenerManager.SelectionSource.MOUSE)
          VimPlugin.getVisualMotion().controlNonVimSelectionChange(editor, VimListenerManager.SelectionSource.MOUSE)
          moveCaretOneCharLeftFromSelectionEnd(editor, predictedMode)
          caret.vimLastColumn = editor.caretModel.visualPosition.column
        }

        mouseDragging = false
      }
    }

    override fun mouseClicked(event: EditorMouseEvent) {
      logger.debug("Mouse clicked")

      if (event.area == EditorMouseEventArea.EDITING_AREA) {
        VimPlugin.getMotion()
        val editor = event.editor
        if (ExEntryPanel.getInstance().isActive) {
          VimPlugin.getProcess().cancelExEntry(editor, false)
        }

        ExOutputModel.getInstance(editor).clear()

        val caretModel = editor.caretModel
        if (editor.subMode != CommandState.SubMode.NONE) {
          caretModel.removeSecondaryCarets()
        }

        if (event.mouseEvent.clickCount == 1) {
          if (editor.inVisualMode) {
            VimPlugin.getVisualMotion().exitVisual(editor)
          } else if (editor.inSelectMode) {
            VimPlugin.getVisualMotion().exitSelectModeAndResetKeyHandler(editor, false)
          }
        }
        // TODO: 2019-03-22 Multi?
        caretModel.primaryCaret.vimLastColumn = caretModel.visualPosition.column
      } else if (event.area != EditorMouseEventArea.ANNOTATIONS_AREA &&
        event.area != EditorMouseEventArea.FOLDING_OUTLINE_AREA &&
        event.mouseEvent.button != MouseEvent.BUTTON3) {
        VimPlugin.getMotion()
        if (ExEntryPanel.getInstance().isActive) {
          VimPlugin.getProcess().cancelExEntry(event.editor, false)
        }

        ExOutputModel.getInstance(event.editor).clear()
      }
    }
  }

  private object ComponentMouseListener : MouseAdapter() {
    override fun mousePressed(e: MouseEvent?) {
      val editor = (e?.component as? EditorComponentImpl)?.editor ?: return
      val predictedMode = VimPlugin.getVisualMotion().predictMode(editor, VimListenerManager.SelectionSource.MOUSE)
      when (e.clickCount) {
        1 -> {
          if (!predictedMode.isEndAllowed) {
            editor.caretModel.runForEachCaret { caret ->
              val lineEnd = EditorHelper.getLineEndForOffset(editor, caret.offset)
              val lineStart = EditorHelper.getLineStartForOffset(editor, caret.offset)
              if (caret.offset == lineEnd && lineEnd != lineStart) {
                caret.moveToOffset(caret.offset - 1)
              }
            }
          }
        }
        2 -> moveCaretOneCharLeftFromSelectionEnd(editor, predictedMode)
      }
    }
  }

  enum class SelectionSource {
    MOUSE,
    OTHER
  }
}
