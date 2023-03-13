/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :argument"
 */
public data class SelectFileCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_IS_COUNT, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val count = getCount(editor, 0, true)

    if (count > 0) {
      val res = injector.file.selectFile(count - 1, context)
      if (res) {
        injector.jumpService.saveJumpLocation(editor)
      }

      return if (res) ExecutionResult.Success else ExecutionResult.Error
    }

    return ExecutionResult.Success
  }
}
