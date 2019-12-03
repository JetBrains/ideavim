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
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.project.Project
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import com.maddyhome.idea.vim.group.visual.moveCaretOneCharLeftFromSelectionEnd
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.option.IdeaRefactorMode
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

/**
 * @author Alex Plate
 */
object IdeaSpecifics {
  fun addIdeaSpecificsListeners(project: Project) {
    EventFacade.getInstance().connectAnActionListener(project, VimActionListener)
    EventFacade.getInstance().connectTemplateStartedListener(project, VimTemplateManagerListener)
    EventFacade.getInstance().connectFindModelListener(project, VimFindModelListener)
    EventFacade.getInstance().registerLookupListener(project, LookupListener)
  }

  fun removeIdeaSpecificsListeners(project: Project) {
    EventFacade.getInstance().removeLookupListener(project, LookupListener)
  }

  private object VimActionListener : AnActionListener {
    private val surrounderItems = listOf("if", "if / else", "for")
    private val surrounderAction = "com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler\$InvokeSurrounderAction"
    private var editor: Editor? = null
    override fun beforeActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
      editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
    }

    override fun afterActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
      //region Extend Selection for Rider
      when (ActionManager.getInstance().getId(action)) {
        IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET, IdeActions.ACTION_EDITOR_UNSELECT_WORD_AT_CARET -> {
          // Rider moves caret to the end of selection
          editor?.caretModel?.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
              val predictedMode = IdeaSelectionControl.predictMode(event.editor, VimListenerManager.SelectionSource.OTHER)
              moveCaretOneCharLeftFromSelectionEnd(event.editor, predictedMode)
              event.editor.caretModel.removeCaretListener(this)
            }
          })
        }
      }
      //endregion

      //region Enter insert mode after surround with if
      if (surrounderAction == action.javaClass.name && surrounderItems.any { action.templatePresentation.text.endsWith(it) }) {
        editor?.let {
          val commandState = editor.commandState
          while (commandState.mode != CommandState.Mode.COMMAND) {
            commandState.popState()
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
  private object VimTemplateManagerListener : TemplateManagerListener {
    override fun templateStarted(state: TemplateState) {
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
            VimPlugin.getChange().insertBeforeCursor(editor, EditorDataContext(editor))
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
      if (evt != null && evt.propertyName == "activeLookup" && evt.oldValue == null && evt.newValue != null) {
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
  private object VimFindModelListener : FindModelListener {
    override fun findNextModelChanged() {
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
}
