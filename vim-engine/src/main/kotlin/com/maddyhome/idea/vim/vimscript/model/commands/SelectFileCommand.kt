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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :argument"
 */
@ExCommand(command = "argu[ment]")
data class SelectFileCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_IS_COUNT, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val count = getCountFromArgument() ?: getCountFromRange(editor, editor.currentCaret())
    if (count <= 0) {
      // Should never get this. The default address for RANGE_IS_COUNT is 1. But it ensures (count - 1) below is safe
      throw exExceptionMessage("E939")  // E939: Positive count required
    }

    val res = injector.file.selectFile(count - 1, context)
    if (res) {
      injector.jumpService.saveJumpLocation(editor)
    }

    return if (res) ExecutionResult.Success else ExecutionResult.Error
  }
}
