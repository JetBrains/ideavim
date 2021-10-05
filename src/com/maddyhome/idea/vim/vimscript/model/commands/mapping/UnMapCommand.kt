/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.mapping

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.commands.Command

data class UnMapCommand(val ranges: Ranges, val argument: String, val cmd: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)

  override fun processCommand(editor: Editor, context: DataContext): ExecutionResult {
    return if (executeCommand()) ExecutionResult.Success else ExecutionResult.Error
  }

  private fun executeCommand(): Boolean {
    val commandInfo = COMMAND_INFOS.find { cmd.startsWith(it.prefix) } ?: return false

    if (argument.isEmpty()) return false

    val parsedKeys = parseKeys(argument.trimStart())

    VimPlugin.getKey().removeKeyMapping(commandInfo.mappingModes, parsedKeys)

    return true
  }

  companion object {
    private val COMMAND_INFOS = arrayOf(
      CommandInfo("unm", "ap", MappingMode.NVO, false),
      CommandInfo("nun", "map", MappingMode.N, false),
      CommandInfo("vu", "nmap", MappingMode.V, false),
      CommandInfo("xu", "nmap", MappingMode.X, false),
      CommandInfo("sunm", "ap", MappingMode.S, false),
      CommandInfo("ou", "nmap", MappingMode.O, false),
      CommandInfo("iu", "nmap", MappingMode.I, false),
      CommandInfo("cu", "nmap", MappingMode.C, false)
    )
  }
}
