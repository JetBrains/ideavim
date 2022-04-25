/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.helper

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsContexts
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.NativeAction
import com.maddyhome.idea.vim.api.VimActionExecutor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.newapi.IjNativeAction
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.annotations.NonNls
import javax.swing.SwingUtilities

@Service
class IjActionExecutor : VimActionExecutor {
  override val ACTION_EDITOR_NEXT_TEMPLATE_VARIABLE: String
    get() = IdeActions.ACTION_EDITOR_NEXT_TEMPLATE_VARIABLE
  override val ACTION_COLLAPSE_ALL_REGIONS: String
    get() = IdeActions.ACTION_COLLAPSE_ALL_REGIONS
  override val ACTION_COLLAPSE_REGION: String
    get() = IdeActions.ACTION_COLLAPSE_REGION
  override val ACTION_COLLAPSE_REGION_RECURSIVELY: String
    get() = IdeActions.ACTION_COLLAPSE_REGION_RECURSIVELY
  override val ACTION_EXPAND_ALL_REGIONS: String
    get() = IdeActions.ACTION_EXPAND_ALL_REGIONS
  override val ACTION_EXPAND_REGION: String
    get() = IdeActions.ACTION_EXPAND_REGION
  override val ACTION_EXPAND_REGION_RECURSIVELY: String
    get() = IdeActions.ACTION_EXPAND_REGION_RECURSIVELY

  /**
   * Execute an action
   *
   * @param ijAction  The action to execute
   * @param context The context to run it in
   */
  override fun executeAction(action: NativeAction, context: ExecutionContext): Boolean {
    val ijAction = (action as IjNativeAction).action
    val event = AnActionEvent(
      null, context.ij, ActionPlaces.KEYBOARD_SHORTCUT, ijAction.templatePresentation.clone(),
      ActionManager.getInstance(), 0
    )
    // beforeActionPerformedUpdate should be called to update the action. It fixes some rider-specific problems.
    //   because rider use async update method. See VIM-1819.
    // This method executes inside of lastUpdateAndCheckDumb
    // Another related issue: VIM-2604
    if (!ActionUtil.lastUpdateAndCheckDumb(ijAction, event, false)) return false
    if (ijAction is ActionGroup && !canBePerformed(event, ijAction, context.ij)) {
      // Some ActionGroups should not be performed, but shown as a popup
      val popup = JBPopupFactory.getInstance()
        .createActionGroupPopup(event.presentation.text, ijAction, context.ij, false, null, -1)
      val component = context.ij.getData(PlatformDataKeys.CONTEXT_COMPONENT)
      if (component != null) {
        val window = SwingUtilities.getWindowAncestor(component)
        if (window != null) {
          popup.showInCenterOf(window)
        }
        return true
      }
      popup.showInFocusCenter()
      return true
    } else {
      ActionUtil.performActionDumbAwareWithCallbacks(ijAction, event)
      return true
    }
  }

  private fun canBePerformed(event: AnActionEvent, action: ActionGroup, context: DataContext): Boolean {
    val presentation = event.presentation
    return try {
      // [VERSION UPDATE] 221+ Just use Presentation.isPerformGroup
      val method = Presentation::class.java.getMethod("isPerformGroup")
      method.invoke(presentation) as Boolean
    } catch (e: Exception) {
      action.canBePerformed(context)
    }
  }

  /**
   * Execute an action by name
   *
   * @param name    The name of the action to execute
   * @param context The context to run it in
   */
  override fun executeAction(name: @NonNls String, context: ExecutionContext): Boolean {
    val aMgr = ActionManager.getInstance()
    val action = aMgr.getAction(name)
    return action != null && executeAction(IjNativeAction(action), context)
  }

  override fun executeCommand(
    editor: VimEditor?,
    runnable: Runnable,
    name: @NlsContexts.Command String?,
    groupId: Any?,
  ) {
    CommandProcessor.getInstance().executeCommand(editor?.ij?.project, runnable, name, groupId)
  }

  override fun executeEsc(context: ExecutionContext): Boolean {
    return executeAction(IdeActions.ACTION_EDITOR_ESCAPE, context)
  }

  override fun executeVimAction(
    editor: VimEditor,
    cmd: EditorActionHandlerBase,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ) {
    CommandProcessor.getInstance()
      .executeCommand(
        editor.ij.project,
        { cmd.execute(editor, getProjectAwareDataContext(editor.ij, context.ij).vim, operatorArguments) },
        cmd.id, DocCommandGroupId.noneGroupId(editor.ij.document), UndoConfirmationPolicy.DEFAULT,
        editor.ij.document
      )
  }

  // This method is copied from com.intellij.openapi.editor.actionSystem.EditorAction.getProjectAwareDataContext
  private fun getProjectAwareDataContext(
    editor: Editor,
    original: DataContext,
  ): DataContext {
    return if (CommonDataKeys.PROJECT.getData(original) === editor.project) {
      DialogAwareDataContext(original)
    } else DataContext { dataId: String? ->
      if (CommonDataKeys.PROJECT.`is`(dataId)) {
        val project = editor.project
        if (project != null) {
          return@DataContext project
        }
      }
      original.getData(dataId!!)
    }
  }

  // This class is copied from com.intellij.openapi.editor.actionSystem.DialogAwareDataContext.DialogAwareDataContext
  private class DialogAwareDataContext(context: DataContext?) : DataContext {
    private val values: MutableMap<String, Any?> = HashMap()

    init {
      for (key in keys) {
        values[key.name] = key.getData(context!!)
      }
    }

    override fun getData(dataId: @NonNls String): Any? {
      if (values.containsKey(dataId)) {
        return values[dataId]
      }
      val editor = values[CommonDataKeys.EDITOR.name] as Editor?
      return if (editor != null) {
        DataManager.getInstance().getDataContext(editor.contentComponent).getData(dataId)
      } else null
    }

    companion object {
      private val keys = arrayOf<DataKey<*>>(
        CommonDataKeys.PROJECT,
        PlatformCoreDataKeys.PROJECT_FILE_DIRECTORY,
        CommonDataKeys.EDITOR,
        CommonDataKeys.VIRTUAL_FILE,
        CommonDataKeys.PSI_FILE
      )
    }
  }
}
