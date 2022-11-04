/*
 * Copyright 2003-2022 The IdeaVim authors
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
 * see "h :class"
 */
data class FindClassCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)
  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val arg = argument
    if (arg.isNotEmpty()) {
      val res = injector.file.openFile("$arg.java", context)
      if (res) {
        injector.markGroup.saveJumpLocation(editor)
      }

      return if (res) ExecutionResult.Success else ExecutionResult.Error
    }

    injector.application.invokeLater { injector.actionExecutor.executeAction("GotoClass", context) }

    return ExecutionResult.Success
  }
}
