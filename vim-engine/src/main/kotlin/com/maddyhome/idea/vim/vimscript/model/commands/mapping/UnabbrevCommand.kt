/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.mapping

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.CommandModifier

@ExCommand(command = "una[bbreviate],iuna[bbrev],cuna[bbrev]")
data class UnabbrevCommand(val range: Range, val cmd: String, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val variant = UnabbrevVariant.matching(cmd) ?: return ExecutionResult.Error
    val parsedArgument = parseArgument(argument.trim())
    val lhs = parsedArgument.rest.ifEmpty { return ExecutionResult.Error }
    if (parsedArgument.bufferLocal) {
      injector.abbreviationGroup.removeBufferLocalAbbreviation(lhs, variant.modes, editor)
    } else {
      injector.abbreviationGroup.removeAbbreviation(lhs, variant.modes)
    }
    return ExecutionResult.Success
  }

  private enum class UnabbrevVariant(val prefix: String, val modes: Set<MappingMode>) {
    UNABBREVIATE("una", MappingMode.IC),
    IUNABBREV("iuna", MappingMode.I),
    CUNABBREV("cuna", MappingMode.C),
    ;

    companion object {
      fun matching(commandName: String): UnabbrevVariant? = entries.find { commandName.startsWith(it.prefix) }
    }
  }
}
