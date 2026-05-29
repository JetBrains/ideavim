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

@ExCommand(command = "abc[lear],iabc[lear],cabc[lear]")
data class AbbrevClearCommand(val range: Range, val cmd: String, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val variant = AbbrevClearVariant.matching(cmd) ?: return ExecutionResult.Error
    // Only <buffer> is meaningful here. A spurious <expr> is silently ignored to match
    // Vim's lenient parsing of clear commands.
    val (bufferLocal, _) = parseArgument(argument.trim())
    if (bufferLocal) {
      injector.abbreviationGroup.clearBufferLocalAbbreviations(variant.modes, editor)
    } else {
      injector.abbreviationGroup.clearAbbreviations(variant.modes)
    }
    return ExecutionResult.Success
  }

  private enum class AbbrevClearVariant(val prefix: String, val modes: Set<MappingMode>) {
    ABCLEAR("abc", MappingMode.IC),
    IABCLEAR("iabc", MappingMode.I),
    CABCLEAR("cabc", MappingMode.C),
    ;

    companion object {
      fun matching(commandName: String): AbbrevClearVariant? = entries.find { commandName.startsWith(it.prefix) }
    }
  }
}
