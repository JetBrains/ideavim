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
data class AutoCmdCommand(
  val range: Range,
  val modifier: CommandModifier,
  val argument: String,
  val eventNames: List<String> = emptyList(),
  val filePattern: String? = null,
  val commandText: String? = null,
) : Command.SingleExecution(range, modifier, argument) {

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
    if (eventNames.isEmpty() || filePattern == null || commandText == null) {
      return ExecutionResult.Error
    }
    val events = try {
      eventNames.map { AutoCmdEvent.valueOf(it) }
    } catch (_: IllegalArgumentException) {
      return ExecutionResult.Error
    }
    events.forEach { event ->
      injector.autoCmd.registerEventCommand(commandText, event, filePattern)
    }
    return ExecutionResult.Success
  }
}
