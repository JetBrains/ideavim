/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContextWrapper
import com.intellij.openapi.actionSystem.EmptyAction
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.ProxyShortcutSet
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.await
import com.intellij.openapi.util.registry.Registry
import com.maddyhome.idea.vim.RegisterActions
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.NativeAction
import com.maddyhome.idea.vim.api.VimActionExecutor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.newapi.IjNativeAction
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.runFromVimKey
import com.maddyhome.idea.vim.newapi.runningIJAction
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.NonNls
import java.awt.Component
import javax.swing.JComponent

@Service
internal class IjActionExecutor : VimActionExecutor {
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
  override fun executeAction(editor: VimEditor, action: NativeAction, context: ExecutionContext): Boolean {
    val ijAction = (action as IjNativeAction).action

    /**
     * Data context that defines that some action was started from IdeaVim.
     * You can call use [runFromVimKey] key to define if intellij action was started from IdeaVim
     */
    val dataContext = DataContextWrapper(context.ij)
    dataContext.putUserData(runFromVimKey, true)

    val contextComponent = PlatformCoreDataKeys.CONTEXT_COMPONENT.getData(dataContext)
      ?: editor.ij.component

    val result = withRunningAction {
      val result = withStringRegistryOption {
        ActionManager.getInstance()
          .tryToExecute(ijAction, null, contextComponent, ActionPlaces.KEYBOARD_SHORTCUT, true)
      }
      result.wait()
    }
    return result.isDone
  }

  private fun <T> withRunningAction(block: () -> T): T {
    runningIJAction = true
    try {
      return block()
    } finally {
      runningIJAction = false
    }
  }

  private fun ActionCallback.wait(): ActionCallback {
    runBlocking {
      try {
        await()
      } catch (_: RuntimeException) {
        // Nothing
        // The exception happens when the action is rejected
        //  and the exception message explains the reason for rejection
        // At the moment, we don't process this information
      }
    }
    return this
  }

  @Suppress("SameParameterValue")
  private fun <T> withStringRegistryOption(block: () -> T): T {
    val registry = Registry.get("actionSystem.update.beforeActionPerformedUpdate")
    val oldValue = registry.asString()
    registry.setValue("on")
    try {
      return block()
    } finally {
      registry.setValue(oldValue)
    }
  }

  /**
   * Execute an action by name
   *
   * @param name    The name of the action to execute
   * @param context The context to run it in
   */
  override fun executeAction(editor: VimEditor, name: @NonNls String, context: ExecutionContext): Boolean {
    val action = getAction(name, context)
    return action != null && executeAction(editor, IjNativeAction(action), context)
  }
  
  private fun getAction(name: String, context: ExecutionContext): AnAction? {
    val actionManager = ActionManager.getInstance()
    val action = actionManager.getAction(name)
    if (action !is EmptyAction) return action

    // But if the action is an instance of EmptyAction, the fun begins
    var component: Component? = context.ij.getData(PlatformDataKeys.CONTEXT_COMPONENT) ?: return null
    while (component != null) {
      if (component !is JComponent) {
        component = component.parent
        continue
      }

      val listOfActions = ActionUtil.getActions(component)
      if (listOfActions.isEmpty()) {
        component = component.getParent()
        continue
      }

      fun AnAction.getId(): String? {
        return actionManager.getId(this)
          ?: (shortcutSet as? ProxyShortcutSet)?.actionId
      }

      for (action in listOfActions) {
        if (action.getId() == name) {
          return action
        }
      }
      component = component.getParent()
    }
    return null
  }

  override fun executeCommand(
    editor: VimEditor?,
    runnable: Runnable,
    name: @NlsContexts.Command String?,
    groupId: Any?,
  ) {
    CommandProcessor.getInstance().executeCommand(editor?.ij?.project, runnable, name, groupId)
  }

  override fun executeEsc(editor: VimEditor, context: ExecutionContext): Boolean {
    return executeAction(editor, IdeActions.ACTION_EDITOR_ESCAPE, context)
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
        { cmd.execute(editor, context, operatorArguments) },
        cmd.id,
        DocCommandGroupId.noneGroupId(editor.ij.document),
        UndoConfirmationPolicy.DEFAULT,
        editor.ij.document,
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

  companion object {
    private val LOG = logger<IjActionExecutor>()
  }
}
