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
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.commands.Command

@ExCommand(command = "unm[ap],nun[map],vu[nmap],xu[nmap],sunm[ap],ou[nmap],unm[ap],iu[nmap],lu[nmap],cu[nmap]")
public data class UnMapCommand(val range: Range, val argument: String, val cmd: String) : Command.SingleExecution(range, argument) {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    return if (executeCommand()) ExecutionResult.Success else ExecutionResult.Error
  }

  private fun executeCommand(): Boolean {
    val commandInfo = COMMAND_INFOS.find { cmd.startsWith(it.prefix) } ?: return false

    if (argument.isEmpty()) return false

    val parsedKeys = injector.parser.parseKeys(argument.trimStart())

    injector.keyGroup.removeKeyMapping(commandInfo.mappingModes, parsedKeys)

    return true
  }

  public companion object {
    private val COMMAND_INFOS = arrayOf(
      CommandInfo("unm", "ap", MappingMode.NVO, false),
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
