/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.execution.actions.StopAction
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.EmptyAction
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.ex.ActionUtil.performDumbAwareWithCallbacks
import com.intellij.openapi.actionSystem.impl.ProxyShortcutSet
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.registry.Registry
import com.maddyhome.idea.vim.RegisterActions
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.NativeAction
import com.maddyhome.idea.vim.api.VimActionExecutor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.ide.isClionNova
import com.maddyhome.idea.vim.ide.isRider
import com.maddyhome.idea.vim.newapi.IjNativeAction
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.runFromVimKey
import org.jetbrains.annotations.NonNls
import java.awt.Component
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

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
  override val ACTION_EXPAND_COLLAPSE_TOGGLE: String
    // [VERSION UPDATE] 2024.3+ Replace raw "ExpandCollapseToggleAction" with IdeActions.ACTION_EXPAND_COLLAPSE_TOGGLE_REGION from the platform.
    get() = "ExpandCollapseToggleAction"

  var isRunningActionFromVim: Boolean = false

  override fun executeAction(editor: VimEditor?, action: NativeAction, context: ExecutionContext): Boolean {
    val applicationEx = ApplicationManagerEx.getApplicationEx()
    if (ProgressIndicatorUtils.isWriteActionRunningOrPending(applicationEx)) {
      // This is needed for VIM-3376
      thisLogger().error("Actions cannot be updated when write-action is running or pending")
    }

    val ijAction = (action as IjNativeAction).action
    if (executeManually(ijAction)) {
      return manualActionExecution(context, ijAction)
    } else {
      try {
        isRunningActionFromVim = true
        // The context component should be editor. This is especially important when running the `:action` commands
        //  because at the moment of execution, the focused component is Ex Field, not editor.
        val contextComponent = editor?.ij?.contentComponent
        val place = ijAction.choosePlace()
        val res = ActionManager.getInstance().tryToExecute(ijAction, null, contextComponent, place, true)
        res.waitFor(5_000)
        return res.isDone
      } finally {
        isRunningActionFromVim = false
      }
    }
  }

  // Note: We should find a proper place for the IdeaVim actions
  // Currently, we use "IdeaVim" except a few actions
  private fun AnAction.choosePlace(): String {
    // StopAction works fine if `StopAction.isPlaceGlobal` returns true
    // Or if there is a specific data stored in the context. This data, however, is stored
    //   only if the run window is in focus.
    if (this is StopAction) return ActionPlaces.ACTION_SEARCH
    return "IdeaVim"
  }

  // [VERSION UPDATE] 251+ Remove manual execution, switch to tryToExecute
  private fun executeManually(action: AnAction): Boolean {
    if (Registry.`is`("ideavim.old.action.execution", true)) return true
    if (isClionNova()) {
      if (action.isEnter() || action.isEsc()) return false
      return true
    }
    if (isRider()) {
      // Special Rider logic for VIM-3826. In rider 251 everything works fine with tryToExecute
      val lessThan251 = ApplicationInfo.getInstance().build.baselineVersion < 251
      val keyIsEnter = action.isEnter()
      val keyIsEsc = action.isEsc()
      if (lessThan251 && !keyIsEnter && !keyIsEsc) return true
    }

    return false
  }

  private fun AnAction.isEsc(): Boolean = this.shortcutSet.shortcuts.any {
    (it as? KeyboardShortcut)?.firstKeyStroke == KeyStroke.getKeyStroke(
      KeyEvent.VK_ESCAPE,
      0
    )
  }

  private fun AnAction.isEnter(): Boolean = this.shortcutSet.shortcuts.any {
    (it as? KeyboardShortcut)?.firstKeyStroke == KeyStroke.getKeyStroke(
      KeyEvent.VK_ENTER,
      0
    )
  }

  private fun manualActionExecution(
    context: ExecutionContext,
    ijAction: AnAction,
  ): Boolean {
    /**
     * Data context that defines that some action was started from IdeaVim.
     * You can call use [runFromVimKey] key to define if intellij action was started from IdeaVim
     */
    val dataContext = SimpleDataContext.getSimpleContext(runFromVimKey, true, context.ij)

    val actionId = ActionManager.getInstance().getId(ijAction)
    @Suppress("removal", "DEPRECATION") val event = AnActionEvent(
      null,
      dataContext,
      ActionPlaces.KEYBOARD_SHORTCUT,
      ijAction.templatePresentation.clone(),
      ActionManager.getInstance(),
      0,
    )
    // beforeActionPerformedUpdate should be called to update the action. It fixes some rider-specific problems
    //   because rider uses an async update method. See VIM-1819.
    // This method executes inside lastUpdateAndCheckDumb
    // Another related issue: VIM-2604

    // This is a hack to fix the tests and fix VIM-3332
    // We should get rid of it in VIM-3376
    if (actionId == "RunClass" || actionId == IdeActions.ACTION_COMMENT_LINE || actionId == IdeActions.ACTION_COMMENT_BLOCK) {
      @Suppress("removal", "OverrideOnly", "DEPRECATION")
      ijAction.beforeActionPerformedUpdate(event)
      if (!event.presentation.isEnabled) return false
    } else {
      if (!ActionUtil.lastUpdateAndCheckDumb(ijAction, event, false)) return false
    }
    if (ijAction is ActionGroup && !event.presentation.isPerformGroup) {
      // Some ActionGroups should not be performed but shown as a popup
      val popup = JBPopupFactory.getInstance()
        .createActionGroupPopup(event.presentation.text, ijAction, dataContext, false, null, -1)
      val component = dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT)
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
      performDumbAwareWithCallbacks(ijAction, event) {
        @Suppress("OverrideOnly")
        ijAction.actionPerformed(event)
      }
      return true
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
    executeAction(editor, IdeActions.ACTION_EDITOR_ESCAPE, context)
    return true
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
}
