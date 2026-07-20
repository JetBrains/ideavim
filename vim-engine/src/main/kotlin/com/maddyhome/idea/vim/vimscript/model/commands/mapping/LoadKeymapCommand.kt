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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.commands.Command.SingleExecution
import com.maddyhome.idea.vim.vimscript.model.commands.CommandModifier

@ExCommand(command = "loadk[eymap]")
data class LoadKeymapCommand(val range: Range, val cmd: String, val modifier: CommandModifier, val argument: String) :

  SingleExecution(range, modifier, argument) {
  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {

    if (!injector.vimscriptExecutor.executingFile) {
      throw exExceptionMessage("E105", ":loadkeymap")
    }

    argument.split("\n").filter { isNotWhite(it) }.forEach { line ->
      val parsed = ParseMapCommandArguments.parseKeymapEntry(line)
        ?: throw exExceptionMessage("E474.arg", line)
      injector.keyGroup
        .putKeyMapping(
          setOf(MappingMode.LANG),
          parsed.fromKeys,
          MappingOwner.IdeaVim.InitScript,
          injector.parser.parseKeys(parsed.secondArgument),
          true,
        )
    }
    return ExecutionResult.Success
  }

  private fun isNotWhite(string: String): Boolean = string.trim().isNotEmpty() && !string.startsWith("\"")

  private fun getParts(preprocessedInput: String): List<String> =
    preprocessedInput.split(" ", "\t").dropLastWhile { it.isEmpty() }.take(2)

  override val argFlags: CommandHandlerFlags
    get() = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)
}