/*
 * Copyright 2003-2026 The IdeaVim authors
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
    val args = parseArgument(argument)
    if (args.isFailure) return ExecutionResult.Error
    args.onSuccess { (events, command) ->
      events.forEach { event ->
        injector.autoCmd.registerEventCommand(command, event)
      }
    }
    return ExecutionResult.Success
  }

  fun parseArgument(argument: String): Result<Pair<List<AutoCmdEvent>, String>> {
    val parts = argument.split('*')
    if (parts.size < 2) return Result.failure(IllegalArgumentException("Invalid autocmd syntax"))
    try {
      val eventsString = parts[0].trim()
      val events = eventsString.split(',').map { AutoCmdEvent.valueOf(it.trim()) }
      return Result.success(Pair(events, parts[1].trim()))
    } catch (e: IllegalArgumentException) {
      return Result.failure(e)
    }
  }
}
