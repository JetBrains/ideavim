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
 * see "h :wnext"
 */
@ExCommand(command = "wn[ext]")
public data class WriteNextFileCommand(val range: Range, val argument: String) : Command.SingleExecution(range, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_IS_COUNT, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    // Note that `:[count]wn[ext] [++opt] {file}` does not get count from argument. Vim would parse a count argument to
    // be the file name
    val count = getCountFromRange(editor, editor.currentCaret())

    injector.file.saveFile(context)
    injector.jumpService.saveJumpLocation(editor)
    injector.file.selectNextFile(count, context)

    return ExecutionResult.Success
  }
}
