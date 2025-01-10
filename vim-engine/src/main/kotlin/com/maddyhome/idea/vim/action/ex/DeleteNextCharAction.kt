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
import com.maddyhome.idea.vim.common.Graphemes
import com.maddyhome.idea.vim.handler.VimActionHandler

@CommandOrMotion(keys = ["<DEL>"], modes = [Mode.CMD_LINE])
class DeleteNextCharAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val commandLine = injector.commandLine.getActiveCommandLine() ?: return false
    val caretOffset = commandLine.caret.offset

    val oldText = commandLine.actualText
    if (oldText.isEmpty()) {
      commandLine.close(refocusOwningEditor = true, resetCaret = false)
      return true
    }

    val newText = if (caretOffset == oldText.length) {
      val preEndOffset = Graphemes.prev(oldText, oldText.length) ?: return true
      oldText.substring(0, preEndOffset)
    } else {
      val nextOffset = Graphemes.next(oldText, caretOffset) ?: return true
      oldText.substring(0, caretOffset) + oldText.substring(nextOffset)
    }
    commandLine.setText(newText)

    return true
  }
}
