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

import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManagerListener
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.find.FindModelListener
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.PlatformUtils
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import com.maddyhome.idea.vim.group.visual.moveCaretOneCharLeftFromSelectionEnd
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.helper.getTopLevelEditor
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.option.IdeaRefactorMode
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

/**
 * @author Alex Plate
 */
object IdeaSpecifics {
  fun addIdeaSpecificsListeners(project: Project) {
    EventFacade.getInstance().registerLookupListener(project, LookupListener)
  }

  fun removeIdeaSpecificsListeners(project: Project) {
    EventFacade.getInstance().removeLookupListener(project, LookupListener)
  }

  class VimActionListener : AnActionListener {
    @NonNls
    private val surrounderItems = listOf("if", "if / else", "for")
    private val surrounderAction = "com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler\$InvokeSurrounderAction"
    private var editor: Editor? = null
    override fun beforeActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
      if (!VimPlugin.isEnabled()) return

      val hostEditor = dataContext.getData(CommonDataKeys.HOST_EDITOR)
      if (hostEditor != null) {
        editor = hostEditor
      }

      if (FindActionId.enabled) {
        val id: String? = ActionManager.getInstance().getId(action)
        VimPlugin.getNotifications(dataContext.getData(CommonDataKeys.PROJECT)).notifyActionId(id)
      }
    }

    override fun afterActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
      if (!VimPlugin.isEnabled()) return

      //region Extend Selection for Rider
      if (PlatformUtils.isRider()) {
        when (ActionManager.getInstance().getId(action)) {
          IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET, IdeActions.ACTION_EDITOR_UNSELECT_WORD_AT_CARET -> {
            // Rider moves caret to the end of selection
            editor?.caretModel?.addCaretListener(object : CaretListener {
              override fun caretPositionChanged(event: CaretEvent) {
                val eventEditor = event.editor.getTopLevelEditor()
                val predictedMode = IdeaSelectionControl.predictMode(eventEditor, VimListenerManager.SelectionSource.OTHER)
                moveCaretOneCharLeftFromSelectionEnd(eventEditor, predictedMode)
                eventEditor.caretModel.removeCaretListener(this)
              }
            })
          }
        }
      }
      //endregion

      //region Enter insert mode after surround with if
      if (surrounderAction == action.javaClass.name && surrounderItems.any { action.templatePresentation.text.endsWith(it) }) {
        editor?.let {
          val commandState = editor.commandState
          while (commandState.mode != CommandState.Mode.COMMAND) {
            commandState.popModes()
          }
          VimPlugin.getChange().insertBeforeCursor(it, dataContext)
          KeyHandler.getInstance().reset(it)
        }
      }
      //endregion

      editor = null
    }
  }

  //region Enter insert mode for surround templates without selection
  class VimTemplateManagerListener : TemplateManagerListener {
    override fun templateStarted(state: TemplateState) {
      if (!VimPlugin.isEnabled()) return
      val editor = state.editor ?: return

      state.addTemplateStateListener(object : TemplateEditingAdapter() {
        override fun currentVariableChanged(templateState: TemplateState, template: Template?, oldIndex: Int, newIndex: Int) {
          if (IdeaRefactorMode.keepMode()) {
            IdeaRefactorMode.correctSelection(editor)
          }
        }
      })

      if (IdeaRefactorMode.keepMode()) {
        IdeaRefactorMode.correctSelection(editor)
      } else {
        if (!editor.selectionModel.hasSelection()) {
          // Enable insert mode if there is no selection in template
          // Template with selection is handled by [com.maddyhome.idea.vim.group.visual.VisualMotionGroup.controlNonVimSelectionChange]
          if (editor.inNormalMode) {
            VimPlugin.getChange().insertBeforeCursor(editor, EditorDataContext.init(editor))
            KeyHandler.getInstance().reset(editor)
          }
        }
      }
    }
  }
  //endregion

  //region Register shortcuts for lookup and perform partial reset
  private object LookupListener : PropertyChangeListener {
    override fun propertyChange(evt: PropertyChangeEvent?) {
      if (evt == null) return
      if (evt.propertyName == "activeLookup" && evt.oldValue == null && evt.newValue != null) {
        val lookup = evt.newValue
        if (lookup is LookupImpl) {
          VimPlugin.getKey().registerShortcutsForLookup(lookup)

          lookup.addLookupListener(object : com.intellij.codeInsight.lookup.LookupListener {
            override fun itemSelected(event: LookupEvent) {
              // VIM-1858
              KeyHandler.getInstance().partialReset(lookup.editor)
              lookup.removeLookupListener(this)
            }
          })
        }
      }
    }
  }
  //endregion

  //region Hide Vim search highlights when showing IntelliJ search results
  class VimFindModelListener : FindModelListener {
    override fun findNextModelChanged() {
      if (!VimPlugin.isEnabled()) return
      VimPlugin.getSearch().clearSearchHighlight()
    }
  }
  //endregion

  //region Ace jump
  fun aceJumpActive(): Boolean {
    // This logic should be removed after creating more correct key processing.
    return EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT)
      .javaClass.name.startsWith("org.acejump.")
  }
  //endregion

  //region AppCode templates
  /**
   * A collection of hacks to improve the interaction with fancy AppCode templates
   */
  object AppCodeTemplates {
    private val facedAppCodeTemplate = Key.create<IntRange>("FacedAppCodeTemplate")

    private const val TEMPLATE_START = "<#T##"
    private const val TEMPLATE_END = "#>"

    @JvmStatic
    fun onMovement(
      editor: @NotNull Editor,
      caret: @NotNull Caret,
      toRight: Boolean
    ) {
      if (!PlatformUtils.isAppCode()) return

      val offset = caret.offset
      val offsetRightEnd = offset + TEMPLATE_START.length
      val offsetLeftEnd = offset - 1
      val templateRange = caret.getUserData(facedAppCodeTemplate)
      if (templateRange == null) {
        if (offsetRightEnd < editor.fileSize
          && editor.document.charsSequence.subSequence(offset, offsetRightEnd).toString() == TEMPLATE_START) {
          caret.shake()

          val templateEnd = editor.findTemplateEnd(offset) ?: return

          caret.putUserData(facedAppCodeTemplate, offset..templateEnd)
        }
        if (offsetLeftEnd >= 0
          && offset + 1 <= editor.fileSize
          && editor.document.charsSequence.subSequence(offsetLeftEnd, offset + 1).toString() == TEMPLATE_END) {
          caret.shake()

          val templateStart = editor.findTemplateStart(offsetLeftEnd) ?: return

          caret.putUserData(facedAppCodeTemplate, templateStart..offset)
        }
      } else {
        if (offset in templateRange) {
          if (toRight) {
            caret.moveToOffset(templateRange.last + 1)
          } else {
            caret.moveToOffset(templateRange.first)
          }
        }
        caret.putUserData(facedAppCodeTemplate, null)
        caret.shake()
      }
    }

    fun Editor.appCodeTemplateCaptured(): Boolean {
      if (!PlatformUtils.isAppCode()) return false
      return this.caretModel.allCarets.any { it.getUserData(facedAppCodeTemplate) != null }
    }

    private fun Caret.shake() {
      moveCaretRelatively(1, 0, false, false)
      moveCaretRelatively(-1, 0, false, false)
    }

    private fun Editor.findTemplateEnd(start: Int): Int? {
      val charSequence = this.document.charsSequence
      val length = charSequence.length
      for (i in start until length - 1) {
        if (charSequence[i] == TEMPLATE_END[0] && charSequence[i+1] == TEMPLATE_END[1]) {
          return i + 1
        }
      }
      return null
    }

    private fun Editor.findTemplateStart(start: Int): Int? {
      val charSequence = this.document.charsSequence
      val templateLastIndex = TEMPLATE_START.length
      for (i in start downTo templateLastIndex) {
        if (charSequence.subSequence(i - templateLastIndex, i + 1).toString() == TEMPLATE_START) {
          return i - templateLastIndex
        }
      }
      return null
    }
  }
  //endregion
}

//region Find action ID
class FindActionIdAction : DumbAwareToggleAction() {
  override fun isSelected(e: AnActionEvent): Boolean = FindActionId.enabled

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    FindActionId.enabled = state
  }
}

object FindActionId {
  var enabled = false
}
//endregion
