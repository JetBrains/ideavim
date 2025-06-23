/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.VimActionHandler

@CommandOrMotion(keys = ["<C-W>"], modes = [Mode.CMD_LINE])
class DeletePrevWordAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val commandLine = injector.commandLine.getActiveCommandLine() ?: return false

    val caretOffset = commandLine.caret.offset
    if (caretOffset == 0) return true

    val oldText = commandLine.actualText
    val motion =
      injector.motion.findOffsetOfNextWord(oldText, commandLine.caret.offset, -1, true, editor)
    if (motion is Motion.AbsoluteOffset) {
      commandLine.deleteText(motion.offset, caretOffset - motion.offset)
    }

    return true
  }
}
