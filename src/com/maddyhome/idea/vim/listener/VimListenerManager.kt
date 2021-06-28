/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimKeyListener
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.VimTypedActionHandler
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.group.EditorGroup
import com.maddyhome.idea.vim.group.FileGroup
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.SearchGroup
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import com.maddyhome.idea.vim.group.visual.VimVisualTimer
import com.maddyhome.idea.vim.group.visual.moveCaretOneCharLeftFromSelectionEnd
import com.maddyhome.idea.vim.group.visual.vimSetSystemSelectionSilently
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.GuicursorChangeListener
import com.maddyhome.idea.vim.helper.UpdatesChecker
import com.maddyhome.idea.vim.helper.exitSelectMode
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.forceBarCursor
import com.maddyhome.idea.vim.helper.inSelectMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.helper.localEditors
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.helper.subMode
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.listener.VimListenerManager.EditorListeners.add
import com.maddyhome.idea.vim.listener.VimListenerManager.EditorListeners.remove
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.StrictMode
import com.maddyhome.idea.vim.ui.ShowCmdOptionChangeListener
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

/**
 * @author Alex Plate
 */

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
    fun enable() {
      val typedAction = TypedAction.getInstance()
      if (typedAction.rawHandler !is VimTypedActionHandler) {
        // Actually this if should always be true, but just as protection
        EventFacade.getInstance().setupTypedActionHandler(VimTypedActionHandler(typedAction.rawHandler))
      } else {
        StrictMode.fail("typeAction expected to be non-vim.")
      }

      OptionsManager.number.addOptionChangeListener(EditorGroup.NumberChangeListener.INSTANCE)
      OptionsManager.relativenumber.addOptionChangeListener(EditorGroup.NumberChangeListener.INSTANCE)
      OptionsManager.scrolloff.addOptionChangeListener(MotionGroup.ScrollOptionsChangeListener.INSTANCE)
      OptionsManager.showcmd.addOptionChangeListener(ShowCmdOptionChangeListener)
      OptionsManager.guicursor.addOptionChangeListener(GuicursorChangeListener)

      EventFacade.getInstance().addEditorFactoryListener(VimEditorFactoryListener, VimPlugin.getInstance())
    }

    fun disable() {
      EventFacade.getInstance().restoreTypedActionHandler()

      OptionsManager.number.removeOptionChangeListener(EditorGroup.NumberChangeListener.INSTANCE)
      OptionsManager.relativenumber.removeOptionChangeListener(EditorGroup.NumberChangeListener.INSTANCE)
      OptionsManager.scrolloff.removeOptionChangeListener(MotionGroup.ScrollOptionsChangeListener.INSTANCE)
      OptionsManager.showcmd.removeOptionChangeListener(ShowCmdOptionChangeListener)
      OptionsManager.guicursor.removeOptionChangeListener(GuicursorChangeListener)

      EventFacade.getInstance().removeEditorFactoryListener(VimEditorFactoryListener)
    }
  }

  object ProjectListeners {
    fun add(project: Project) {
      IdeaSpecifics.addIdeaSpecificsListeners(project)
    }

    fun removeAll() {
      // Project listeners are self-disposable, so there is no need to unregister them on project close
      ProjectManager.getInstance().openProjects.filterNot { it.isDisposed }
        .forEach { IdeaSpecifics.removeIdeaSpecificsListeners(it) }
    }

    fun addAll() {
      ProjectManager.getInstance().openProjects.filterNot { it.isDisposed }.forEach { add(it) }
    }
  }

  object EditorListeners {
    fun addAll() {
      localEditors().forEach { editor ->
        this.add(editor)
      }
    }

    fun removeAll() {
      localEditors().forEach { editor ->
        this.remove(editor, false)
      }
    }

    fun add(editor: Editor) {

      editor.contentComponent.addKeyListener(VimKeyListener)
      val eventFacade = EventFacade.getInstance()
      eventFacade.addEditorMouseListener(editor, EditorMouseHandler)
      eventFacade.addEditorMouseMotionListener(editor, EditorMouseHandler)
      eventFacade.addEditorSelectionListener(editor, EditorSelectionHandler)
      eventFacade.addComponentMouseListener(editor.contentComponent, ComponentMouseListener)

      VimPlugin.getEditor().editorCreated(editor)

      VimPlugin.getChange().editorCreated(editor)
    }

    fun remove(editor: Editor, isReleased: Boolean) {

      editor.contentComponent.removeKeyListener(VimKeyListener)
      val eventFacade = EventFacade.getInstance()
      eventFacade.removeEditorMouseListener(editor, EditorMouseHandler)
      eventFacade.removeEditorMouseMotionListener(editor, EditorMouseHandler)
      eventFacade.removeEditorSelectionListener(editor, EditorSelectionHandler)
      eventFacade.removeComponentMouseListener(editor.contentComponent, ComponentMouseListener)

      VimPlugin.getEditorIfCreated()?.editorDeinit(editor, isReleased)

      VimPlugin.getChange().editorReleased(editor)
    }
  }

  class VimFileEditorManagerListener : FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {
      if (!VimPlugin.isEnabled()) return
      MotionGroup.fileEditorManagerSelectionChangedCallback(event)
      FileGroup.fileEditorManagerSelectionChangedCallback(event)
      SearchGroup.fileEditorManagerSelectionChangedCallback(event)
    }
  }

  private object VimEditorFactoryListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
      add(event.editor)
      UpdatesChecker.check()
    }

    override fun editorReleased(event: EditorFactoryEvent) {
      remove(event.editor, true)
      VimPlugin.getMark().editorReleased(event)
    }
  }

  private object EditorSelectionHandler : SelectionListener {
    private var myMakingChanges = false

    /**
     * This event is executed for each caret using [com.intellij.openapi.editor.CaretModel.runForEachCaret]
     */
    override fun selectionChanged(selectionEvent: SelectionEvent) {
      if (selectionEvent.editor.isIdeaVimDisabledHere) return
      val editor = selectionEvent.editor
      val document = editor.document

      if (SelectionVimListenerSuppressor.isNotLocked) {
        logger.debug("Adjust non vim selection change")
        IdeaSelectionControl.controlNonVimSelectionChange(editor)
      }

      if (myMakingChanges || document is DocumentEx && document.isInEventsHandling) {
        return
      }

      myMakingChanges = true
      try {
        // Synchronize selections between editors
        val newRange = selectionEvent.newRange
        for (e in localEditors(document)) {
          if (e != editor) {
            e.selectionModel.vimSetSystemSelectionSilently(newRange.startOffset, newRange.endOffset)
          }
        }
      } finally {
        myMakingChanges = false
      }
    }
  }

  private object EditorMouseHandler : EditorMouseListener, EditorMouseMotionListener {
    private var mouseDragging = false
    private var cutOffFixed = false

    override fun mouseDragged(e: EditorMouseEvent) {
      if (e.editor.isIdeaVimDisabledHere) return

      val caret = e.editor.caretModel.primaryCaret

      if (!mouseDragging) {
        logger.debug("Mouse dragging")
        SelectionVimListenerSuppressor.lock()
        VimVisualTimer.swingTimer?.stop()
        mouseDragging = true

        /**
         * If we make a small drag when moving the caret to the end of a line, it is very easy to get an unexpected
         * selection. This is because the first click moves the caret passed the end of the line, and is then received
         * in [ComponentMouseListener] and the caret is moved back to the start of the last character of the line. If
         * there is a drag, we get a selection of the last character. This check simply removes that selection. We force
         * the bar caret simply because it looks better - the block caret is dragged to the end, becomes a less
         * intrusive bar caret and snaps back to the last character (and block caret) when the mouse is released.
         * TODO: Vim supports selection of the character after the end of line
         * (Both with mouse and with v$. IdeaVim treats v$ as an exclusive selection)
         */
        if (onLineEnd(caret)) {
          caret.removeSelection()
          caret.forceBarCursor()
        }
      }
      if (mouseDragging && caret.hasSelection()) {
        /**
         * We force the bar caret while dragging because it matches IntelliJ's selection model better.
         * * Vim's drag selection is based on character bounding boxes. When 'selection' is set to "inclusive" (the
         *   default), Vim selects a character when the mouse cursor drags the text caret into its bounding box (LTR).
         *   The character at the text caret is selected and the block caret is drawn to cover the character (the bar
         *   caret would be between the selection and the last character of the selection, which is weird). See "v" in
         *   'guicursor'. When 'selection' is "exclusive", Vim will select a character when the mouse cursor drags the
         *   text caret out of its bounding box. The character at the text caret is not selected and the bar caret is
         *   drawn at the start of this character to make it more obvious that it is unselected. See "ve" in
         *   'guicursor'.
         * * IntelliJ's selection is based on character mid-points. E.g. the caret is moved to the start of offset 2
         *   when the second half of offset 1 is clicked, and a character is selected when the mouse is moved from the
         *   first half to the second half. This means:
         *   1) While dragging, the selection is always exclusive - the character at the text caret is not selected. We
         *   convert to an inclusive selection when the mouse is released, by moving back one character. It makes
         *   sense to match Vim's bar caret here.
         *   2) An exclusive selection should trail behind the mouse cursor, but IntelliJ doesn't, because the selection
         *   boundaries are mid points - the text caret can be in front of/to the right of the mouse cursor (LTR).
         *   Using a block caret would push the block further out passed the selection and the mouse cursor, and
         *   feels wrong. The bar caret is a better user experience.
         *   RTL probably introduces other fun issues
         * We can implement inclusive/exclusive 'selection' with normal text movement, but unless we can change the way
         * selection works while dragging, I don't think we can match Vim's selection behaviour exactly.
         */
        caret.forceBarCursor()

        if (!cutOffFixed && ComponentMouseListener.cutOffEnd) {
          cutOffFixed = true
          SelectionVimListenerSuppressor.lock().use {
            if (caret.selectionEnd == e.editor.document.getLineEndOffset(caret.logicalPosition.line) - 1 &&
              caret.leadSelectionOffset == caret.selectionEnd
            ) {
              // A small but important customization. Because IdeaVim doesn't allow to put the caret on the line end,
              //   the selection can omit the last character if the selection was started in the middle on the
              //   last character in line and has a negative direction.
              caret.setSelection(caret.selectionStart, caret.selectionEnd + 1)
            }
          }
        }
      }
    }

    private fun onLineEnd(caret: Caret): Boolean {
      val editor = caret.editor
      val lineEnd = EditorHelper.getLineEndForOffset(editor, caret.offset)
      val lineStart = EditorHelper.getLineStartForOffset(editor, caret.offset)
      return caret.offset == lineEnd && lineEnd != lineStart && caret.offset - 1 == caret.selectionStart && caret.offset == caret.selectionEnd
    }

    // Note that the MacBook's trackpad has a small delay before mouseReleased is received, presumably to allow
    // repositioning fingers to continue a drag operation. This can cause confusion when observing some of the effects
    // in this handler!
    override fun mouseReleased(event: EditorMouseEvent) {
      if (event.editor.isIdeaVimDisabledHere) return
      if (mouseDragging) {
        logger.debug("Release mouse after dragging")
        val editor = event.editor
        val caret = editor.caretModel.primaryCaret
        SelectionVimListenerSuppressor.unlock {
          val predictedMode = IdeaSelectionControl.predictMode(editor, SelectionSource.MOUSE)
          IdeaSelectionControl.controlNonVimSelectionChange(editor, SelectionSource.MOUSE)
          // TODO: This should only be for 'selection'=inclusive
          moveCaretOneCharLeftFromSelectionEnd(editor, predictedMode)

          // Reset caret after forceBarShape while dragging
          editor.updateCaretsVisualAttributes()
          caret.vimLastColumn = editor.caretModel.visualPosition.column
        }

        mouseDragging = false
        cutOffFixed = false
      }
    }

    override fun mouseClicked(event: EditorMouseEvent) {
      if (event.editor.isIdeaVimDisabledHere) return
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
            editor.exitVisualMode()
          } else if (editor.inSelectMode) {
            editor.exitSelectMode(false)
            KeyHandler.getInstance().reset(editor)
          }
        }
        // TODO: 2019-03-22 Multi?
        caretModel.primaryCaret.vimLastColumn = caretModel.visualPosition.column
      } else if (event.area != EditorMouseEventArea.ANNOTATIONS_AREA &&
        event.area != EditorMouseEventArea.FOLDING_OUTLINE_AREA &&
        event.mouseEvent.button != MouseEvent.BUTTON3
      ) {
        VimPlugin.getMotion()
        if (ExEntryPanel.getInstance().isActive) {
          VimPlugin.getProcess().cancelExEntry(event.editor, false)
        }

        ExOutputModel.getInstance(event.editor).clear()
      }
    }
  }

  private object ComponentMouseListener : MouseAdapter() {

    var cutOffEnd = false

    override fun mousePressed(e: MouseEvent?) {
      val editor = (e?.component as? EditorComponentImpl)?.editor ?: return
      if (editor.isIdeaVimDisabledHere) return
      val predictedMode = IdeaSelectionControl.predictMode(editor, SelectionSource.MOUSE)
      when (e.clickCount) {
        1 -> {
          if (!predictedMode.isEndAllowed) {
            editor.caretModel.runForEachCaret { caret ->
              val lineEnd = EditorHelper.getLineEndForOffset(editor, caret.offset)
              val lineStart = EditorHelper.getLineStartForOffset(editor, caret.offset)
              cutOffEnd = if (caret.offset == lineEnd && lineEnd != lineStart) {
                caret.moveToInlayAwareOffset(caret.offset - 1)
                true
              } else {
                false
              }
            }
          } else cutOffEnd = false
        }
        // TODO: Modify this to support 'selection' set to "exclusive"
        // When 'selection' is "inclusive" (default), double clicking a word in Vim will include the last character of
        // the selection, so move the caret back one character. This also means the block caret is drawn "over" the last
        // character, rather than over the next character.
        // When 'selection' is "exclusive", the selection doesn't include the last character, so the caret should not be
        // moved. Vim uses a bar caret when in VISUAL mode, so the caret is shown after the end of the selection, but
        // not "over" the next character.
        2 -> moveCaretOneCharLeftFromSelectionEnd(editor, predictedMode)
      }
    }
  }

  enum class SelectionSource {
    MOUSE,
    OTHER
  }
}
