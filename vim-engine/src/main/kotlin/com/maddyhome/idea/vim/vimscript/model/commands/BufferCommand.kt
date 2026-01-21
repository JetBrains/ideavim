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
 * see "h :buffer"
 */
@ExCommand(command = "b[uffer]")
data class BufferCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_IS_COUNT, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val arg = argument.trim()

    // Try to parse as a number first
    val bufNum = arg.toIntOrNull()
    if (bufNum != null) {
      if (bufNum <= 0) {
        throw exExceptionMessage("E939")
      }
      val res = injector.file.selectFile(bufNum - 1, context)
      if (res) {
        injector.jumpService.saveJumpLocation(editor)
      }
      return if (res) ExecutionResult.Success else ExecutionResult.Error
    }

    // If no argument or not a number, try to select by name
    if (arg.isNotEmpty()) {
      val res = injector.file.openFile(arg, context)
      if (res) {
        injector.jumpService.saveJumpLocation(editor)
      }
      return if (res) ExecutionResult.Success else ExecutionResult.Error
    }

    // If no argument provided, use count from range
    val count = getCountFromRange(editor, editor.currentCaret())
    if (count <= 0) {
      throw exExceptionMessage("E939")
    }
    val res = injector.file.selectFile(count - 1, context)
    if (res) {
      injector.jumpService.saveJumpLocation(editor)
    }
    return if (res) ExecutionResult.Success else ExecutionResult.Error
  }
}
