/*
 * Copyright 2003-2023 The IdeaVim authors
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
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.CommandModifier

@ExCommand(command = "unm[ap],nun[map],vu[nmap],xu[nmap],sunm[ap],ou[nmap],iu[nmap],lu[nmap],cu[nmap]")
data class UnMapCommand(val range: Range, val cmd: String, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    return if (executeCommand()) ExecutionResult.Success else ExecutionResult.Error
  }

  private fun executeCommand(): Boolean {
    val bang = modifier == CommandModifier.BANG
    val commandInfo = COMMAND_INFOS.find { cmd.startsWith(it.prefix) && it.bang == bang }
    if (commandInfo == null) {
      if (modifier == CommandModifier.BANG) throw exExceptionMessage("E477")
      return false
    }

    if (argument.isEmpty()) return false

    val parsedKeys = injector.parser.parseKeys(argument.trimStart())

    injector.keyGroup.removeKeyMapping(commandInfo.mappingModes, parsedKeys)

    return true
  }

  companion object {
    private val COMMAND_INFOS = arrayOf(
      // TODO: Support lunmap
      CommandInfo("unm", "ap", MappingMode.NVO, false),
      CommandInfo("unm", "ap", MappingMode.IC, false, bang = true),
      CommandInfo("nun", "map", MappingMode.N, false),
      CommandInfo("vu", "nmap", MappingMode.V, false),
      CommandInfo("xu", "nmap", MappingMode.X, false),
      CommandInfo("sunm", "ap", MappingMode.S, false),
      CommandInfo("ou", "nmap", MappingMode.O, false),
      CommandInfo("iu", "nmap", MappingMode.I, false),
      CommandInfo("cu", "nmap", MappingMode.C, false),
    )
  }
}
