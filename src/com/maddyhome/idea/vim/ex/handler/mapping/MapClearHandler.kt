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

package com.maddyhome.idea.vim.ex.handler.mapping

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.CommandHandler.Access.READ_ONLY
import com.maddyhome.idea.vim.ex.CommandHandler.ArgumentFlag.ARGUMENT_FORBIDDEN
import com.maddyhome.idea.vim.ex.CommandHandler.RangeFlag.RANGE_FORBIDDEN
import com.maddyhome.idea.vim.ex.CommandHandlerFlags
import com.maddyhome.idea.vim.ex.CommandName
import com.maddyhome.idea.vim.ex.ComplicatedNameExCommand
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.commands
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.ex.vimscript.VimScriptCommandHandler

class MapClearHandler : CommandHandler.SingleExecution(), VimScriptCommandHandler, ComplicatedNameExCommand {
  override val argFlags: CommandHandlerFlags = flags(RANGE_FORBIDDEN, ARGUMENT_FORBIDDEN, READ_ONLY)
  override val names: Array<CommandName> = COMMAND_NAMES

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean = executeCommand(cmd)

  override fun execute(cmd: ExCommand) {
    executeCommand(cmd)
  }

  private fun executeCommand(cmd: ExCommand): Boolean {
    val commandInfo = COMMAND_INFOS.find { cmd.command.startsWith(it.prefix) } ?: return false

    VimPlugin.getKey().removeKeyMapping(commandInfo.mappingModes)

    return true
  }

  companion object {
    private val COMMAND_INFOS = arrayOf(
      CommandInfo("mapc", "lear", MappingMode.NVO, false),
      CommandInfo("nmapc", "lear", MappingMode.N, false),
      CommandInfo("vmapc", "lear", MappingMode.V, false),
      CommandInfo("xmapc", "lear", MappingMode.X, false),
      CommandInfo("smapc", "lear", MappingMode.S, false),
      CommandInfo("omapc", "lear", MappingMode.O, false),
      CommandInfo("imapc", "lear", MappingMode.I, false),
      CommandInfo("cmapc", "lear", MappingMode.C, false)
    )

    val COMMAND_NAMES = commands(*COMMAND_INFOS.map { it.command }.toTypedArray())
  }
}
