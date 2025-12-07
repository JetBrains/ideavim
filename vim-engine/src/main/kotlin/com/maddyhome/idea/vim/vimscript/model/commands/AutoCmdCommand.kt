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
import com.maddyhome.idea.vim.autocmd.AutoCmdEvent
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

@ExCommand(command = "au[tocmd]")
data class AutoCmdCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    if (modifier == CommandModifier.BANG) {
      injector.autoCmd.clearEvents()
      return ExecutionResult.Success
    }
    val args = parseArgument(argument).onSuccess {
      injector.autoCmd.registerEventCommand(it.second, it.first)
    }
    if (args.isFailure) return ExecutionResult.Error
    return ExecutionResult.Success
  }


  fun parseArgument(argument: String): Result<Pair<AutoCmdEvent, String>> {
    val parts = argument.split('*')
    try {
      val command = AutoCmdEvent.valueOf(parts[0].trim())
      return Result.success(Pair(command, parts[1].trim()))
    } catch (e: IllegalArgumentException) {
     return Result.failure(e)
    }
  }
}
