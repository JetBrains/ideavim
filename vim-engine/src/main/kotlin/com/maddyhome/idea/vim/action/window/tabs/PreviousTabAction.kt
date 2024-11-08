/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.window.tabs

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

@CommandOrMotion(keys = ["gT", "<C-PageUp>"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class PreviousTabAction : VimActionHandler.SingleExecution() {
  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.motion.moveCaretGotoPreviousTab(editor, context, cmd.rawCount)
    return true
  }

  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED
}

@CommandOrMotion(keys = ["<C-PageUp>"], modes = [Mode.INSERT])
class InsertPreviousTabAction : VimActionHandler.SingleExecution() {
  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.motion.moveCaretGotoPreviousTab(editor, context, cmd.rawCount)
    return true
  }

  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED
}
