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

@ExCommand(command = "ab[breviate],ia[bbrev],ca[bbrev],norea[bbrev],inorea[bbrev],cnorea[bbrev]")
data class AbbrevCommand(val range: Range, val cmd: String, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val variant = AbbrevVariant.matching(cmd) ?: return ExecutionResult.Error

    val trimmedArgument = argument.trim()
    if (isListRequest(trimmedArgument)) return ExecutionResult.Success

    val lhs = trimmedArgument.substringBefore(' ')
    val rhs = trimmedArgument.substringAfter(' ').trimStart()
    injector.abbreviationGroup.setAbbreviation(lhs, rhs, variant.modes, variant.recursive)
    return ExecutionResult.Success
  }

  private fun isListRequest(trimmedArgument: String): Boolean =
    trimmedArgument.isEmpty() || !trimmedArgument.contains(' ')

  private enum class AbbrevVariant(val prefix: String, val modes: Set<MappingMode>, val recursive: Boolean) {
    ABBREVIATE("ab", MappingMode.IC, true),
    IABBREV("ia", MappingMode.I, true),
    CABBREV("ca", MappingMode.C, true),
    NOREABBREV("norea", MappingMode.IC, false),
    INOREABBREV("inorea", MappingMode.I, false),
    CNOREABBREV("cnorea", MappingMode.C, false),
    ;

    companion object {
      fun matching(commandName: String): AbbrevVariant? = entries.find { commandName.startsWith(it.prefix) }
    }
  }
}
