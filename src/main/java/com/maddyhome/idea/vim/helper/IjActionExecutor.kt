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

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.SlowOperations
import com.maddyhome.idea.vim.RegisterActions
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
    if (ijAction is ActionGroup && !event.presentation.isPerformGroup) {
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
      performDumbAwareWithCallbacks(ijAction, event) { ijAction.actionPerformed(event) }
      return true
    }
  }

  // This is taken directly from ActionUtil.performActionDumbAwareWithCallbacks
  // But with one check removed. With this check some actions (like `:w` doesn't work)
  // https://youtrack.jetbrains.com/issue/VIM-2691/File-is-not-saved-on-w
  private fun performDumbAwareWithCallbacks(
    action: AnAction,
    event: AnActionEvent,
    performRunnable: Runnable,
  ) {
    val project = event.project
    var indexError: IndexNotReadyException? = null
    val manager = ActionManagerEx.getInstanceEx()
    manager.fireBeforeActionPerformed(action, event)
    var result: AnActionResult? = null
    try {
      SlowOperations.allowSlowOperations(SlowOperations.ACTION_PERFORM).use {
        performRunnable.run()
        result = AnActionResult.PERFORMED
      }
    } catch (ex: IndexNotReadyException) {
      indexError = ex
      result = AnActionResult.failed(ex)
    } catch (ex: RuntimeException) {
      result = AnActionResult.failed(ex)
      throw ex
    } catch (ex: Error) {
      result = AnActionResult.failed(ex)
      throw ex
    } finally {
      if (result == null) result = AnActionResult.failed(Throwable())
      manager.fireAfterActionPerformed(action, event, result!!)
    }
    if (indexError != null) {
      ActionUtil.showDumbModeWarning(project, event)
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
        { cmd.execute(editor, EditorDataContext.init(editor.ij, context.ij).vim, operatorArguments) },
        cmd.id, DocCommandGroupId.noneGroupId(editor.ij.document), UndoConfirmationPolicy.DEFAULT,
        editor.ij.document
      )
  }

  override fun findVimAction(id: String): EditorActionHandlerBase? {
    return RegisterActions.findAction(id)
  }

  override fun findVimActionOrDie(id: String): EditorActionHandlerBase {
    return RegisterActions.findActionOrDie(id)
  }

  override fun getAction(actionId: String): NativeAction? {
    return ActionManager.getInstance().getAction(actionId)?.let { IjNativeAction(it) }
  }

  override fun getActionIdList(idPrefix: String): List<String> {
    return ActionManager.getInstance().getActionIdList(idPrefix)
  }
}
