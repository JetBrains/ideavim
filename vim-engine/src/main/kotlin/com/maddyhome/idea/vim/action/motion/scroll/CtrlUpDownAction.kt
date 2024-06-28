/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.scroll

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

/**
 * @author Alex Plate
 */
// FIXME: 2019-07-05 Workaround to make jump through methods work
@CommandOrMotion(keys = ["<C-Down>"], modes = [Mode.NORMAL])
class CtrlDownAction : VimActionHandler.SingleExecution() {

  private val keySet = parseKeysSet("<C-Down>")

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val keyStroke = keySet.first().first()
    val actions = injector.keyGroup.getKeymapConflicts(keyStroke)
    for (action in actions) {
      if (injector.actionExecutor.executeAction(editor, action, context)) break
    }
    return true
  }
}

@CommandOrMotion(keys = ["<C-Up>"], modes = [Mode.NORMAL])
class CtrlUpAction : VimActionHandler.SingleExecution() {

  private val keySet = parseKeysSet("<C-Up>")

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val keyStroke = keySet.first().first()
    val actions = injector.keyGroup.getKeymapConflicts(keyStroke)
    for (action in actions) {
      if (injector.actionExecutor.executeAction(editor, action, context)) break
    }
    return true
  }
}
