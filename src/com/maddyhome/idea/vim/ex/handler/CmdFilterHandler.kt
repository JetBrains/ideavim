/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.*
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.Msg
import java.io.IOException

class CmdFilterHandler : CommandHandler.SingleExecution() {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.WRITABLE)
  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    logger.info("execute")

    var command = cmd.argument
    if (command.isEmpty()) {
      return false
    }

    if ('!' in command) {
      val last = VimPlugin.getProcess().lastCommand
      if (last.isNullOrEmpty()) {
        VimPlugin.showMessage(MessageHelper.message(Msg.e_noprev))
        return false
      }
      command = command.replace("!".toRegex(), last)
    }

    return try {
      if (cmd.ranges.size() == 0) {
        // Show command output in a window
        val commandOutput = VimPlugin.getProcess().executeCommand(command, null)
        ExOutputModel.getInstance(editor).output(commandOutput)
        true
      } else {
        // Filter
        val range = cmd.getTextRange(editor, context, false)
        VimPlugin.getProcess().executeFilter(editor, range, command)
      }
    } catch (e: IOException) {
      throw ExException(e.message)
    }
  }

  companion object {
    private val logger = Logger.getInstance(CmdFilterHandler::class.java.name)
  }
}
