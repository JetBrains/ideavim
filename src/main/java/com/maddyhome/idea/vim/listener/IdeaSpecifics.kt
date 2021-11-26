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

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupManagerListener
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
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareToggleAction
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.helper.EditorDataContext
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.vimscript.model.options.helpers.IdeaRefactorModeHelper
import org.jetbrains.annotations.NonNls

/**
 * @author Alex Plate
 */
object IdeaSpecifics {
  class VimActionListener : AnActionListener {
    @NonNls
    private val surrounderItems = listOf("if", "if / else", "for")
    private val surrounderAction =
      "com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler\$InvokeSurrounderAction"
    private var editor: Editor? = null
    override fun beforeActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
      if (!VimPlugin.isEnabled()) return

      val hostEditor = dataContext.getData(CommonDataKeys.HOST_EDITOR)
      if (hostEditor != null) {
        editor = hostEditor
      }

      //region Track action id
      if (FindActionId.enabled) {
        if (action !is NotificationService.ActionIdNotifier.CopyActionId && action !is NotificationService.ActionIdNotifier.StopTracking) {
          val id: String? = ActionManager.getInstance().getId(action)
          VimPlugin.getNotifications(dataContext.getData(CommonDataKeys.PROJECT)).notifyActionId(id)
        }
      }
      //endregion
    }

    override fun afterActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
      if (!VimPlugin.isEnabled()) return

      //region Enter insert mode after surround with if
      if (surrounderAction == action.javaClass.name && surrounderItems.any {
        action.templatePresentation.text.endsWith(
            it
          )
      }
      ) {
        editor?.let {
          val commandState = it.commandState
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
        override fun currentVariableChanged(
          templateState: TemplateState,
          template: Template?,
          oldIndex: Int,
          newIndex: Int,
        ) {
          if (IdeaRefactorModeHelper.keepMode()) {
            IdeaRefactorModeHelper.correctSelection(editor)
          }
        }
      })

      if (IdeaRefactorModeHelper.keepMode()) {
        IdeaRefactorModeHelper.correctSelection(editor)
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
  class LookupTopicListener : LookupManagerListener {
    override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
      if (!VimPlugin.isEnabled()) return

      // Lookup opened
      if (oldLookup == null && newLookup is LookupImpl) {
        if (newLookup.editor.isIdeaVimDisabledHere) return

        VimPlugin.getKey().registerShortcutsForLookup(newLookup)
      }

      // Lookup closed
      if (oldLookup != null && newLookup == null) {
        val editor = oldLookup.editor
        if (editor.isIdeaVimDisabledHere) return
        // VIM-1858
        KeyHandler.getInstance().partialReset(editor)
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
