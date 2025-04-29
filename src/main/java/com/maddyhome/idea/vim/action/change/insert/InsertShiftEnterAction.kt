/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change.insert

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.newapi.IjNativeAction
import com.maddyhome.idea.vim.newapi.ij
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke

@CommandOrMotion(keys = ["<S-CR>"], modes = [Mode.INSERT])
class InsertShiftEnterAction : VimActionHandler.SingleExecution() {
  private val keySet = parseKeysSet("<S-CR>")

  override val type: Command.Type = Command.Type.INSERT

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {

    // Vim has no commands on shift-enter. However, to properly support IJ actions on this shortcut, we register
    //   a command in IdeaVim and dispatch it to the IJ. This is, however, incorrect, and a different approach
    //   should be used which doesn't pass an execution flow to vim where this is in applicable.
    // Short notes on this situation: https://youtrack.jetbrains.com/issue/VIM-3786/Inconsistent-Shortcut-for-Line-Break-in-AI-Assistant-Chat-with-IdeaVim-Plugin#focus=Comments-27-11963546.0-0

    if (EditorHelper.getVirtualFile(editor.ij)?.name?.contains("AIAssistantInput") == true) {
      // In the case of AI assistant, we get a conflict. Both Vim and AI shift-enter actions are
      //   registered not as global actions, but as component-level actions. However, AI is registered
      //   as a higher-level component, so IJ selects Vim action. Disabling Vim action doesn't help as
      //   it will be selected anyway. Here, we search for the shift-enter action for AI and run it manually.
      val actions = findAiAction(editor.ij.contentComponent.parent as? JComponent)
      for (action in actions) {
        val nativeAction = IjNativeAction(action)
        if (injector.actionExecutor.executeAction(editor, nativeAction, context)) break
      }
    }

    val keyStroke = keySet.first().first()
    val actions = injector.keyGroup.getKeymapConflicts(keyStroke)
    for (action in actions) {
      if (injector.actionExecutor.executeAction(editor, action, context)) break
    }
    return true
  }

  private fun findAiAction(component: JComponent?): List<AnAction> {
    if (component == null) return emptyList()
    val enterShortcut = KeyboardShortcut(
      KeyStroke.getKeyStroke(
        KeyEvent.VK_ENTER,
        KeyEvent.SHIFT_DOWN_MASK
      ), null
    )
    val actions = ActionUtil.getActions(component)
      .filter { it.shortcutSet.shortcuts.any { shortcut -> shortcut.startsWith(enterShortcut) } }
    if (actions.isNotEmpty()) {
      return actions
    } else {
      val parent = component.parent
      return findAiAction(parent as? JComponent)
    }
  }
}