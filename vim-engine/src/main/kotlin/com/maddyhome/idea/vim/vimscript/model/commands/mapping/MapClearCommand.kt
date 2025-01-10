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

@ExCommand(command = "mapc[lear],nmapc[lear],vmapc[lear],xmapc[lear],smapc[lear],omapc[lear],imapc[lear],lmapc[lear],cmapc[lear]")
data class MapClearCommand(val range: Range, val cmd: String, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_FORBIDDEN, Access.READ_ONLY)

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
      if (modifier == CommandModifier.BANG) throw exExceptionMessage("E477")  // E477: No ! allowed
      return false
    }

    injector.keyGroup.removeKeyMapping(commandInfo.mappingModes)

    return true
  }

  companion object {
    private val COMMAND_INFOS = arrayOf(
      // TODO: Support lmapclear
      CommandInfo("mapc", "lear", MappingMode.NVO, false),
      CommandInfo("mapc", "lear", MappingMode.IC, false, bang = true),
      CommandInfo("nmapc", "lear", MappingMode.N, false),
      CommandInfo("vmapc", "lear", MappingMode.V, false),
      CommandInfo("xmapc", "lear", MappingMode.X, false),
      CommandInfo("smapc", "lear", MappingMode.S, false),
      CommandInfo("omapc", "lear", MappingMode.O, false),
      CommandInfo("imapc", "lear", MappingMode.I, false),
      CommandInfo("cmapc", "lear", MappingMode.C, false),
    )
  }
}
