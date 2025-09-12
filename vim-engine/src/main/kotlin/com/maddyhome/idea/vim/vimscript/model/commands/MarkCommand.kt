/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :mark"
 */
@ExCommand(command = "k,ma[rk]")
data class MarkCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)

  // todo make it multicaret
  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val mark = argument[0]
    val line = getLine(editor)
    val offset = editor.getLineStartOffset(line)

    val result = if (mark.isLetter() || mark in "'`") {
      injector.markService.setMark(editor.primaryCaret(), mark, offset)
    } else {
      injector.messages.showStatusBarMessage(editor, injector.messages.message("E191"))
      false
    }
    return if (result) ExecutionResult.Success else ExecutionResult.Error
  }
}
