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
package com.maddyhome.idea.vim.action.change

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.handler.VimActionHandler

class RepeatChangeAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_WRITABLE

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val state = CommandState.getInstance(editor)
    val lastCommand = VimRepeater.lastChangeCommand ?: return false

    if (cmd.rawCount > 0) {
      lastCommand.count = cmd.count
      val arg = lastCommand.argument
      if (arg != null) {
        val mot = arg.motion
        mot.count = 0
      }
    }

    // Save state
    val save = state.command
    val lastFTCmd = VimPlugin.getMotion().lastFTCmd
    val lastFTChar = VimPlugin.getMotion().lastFTChar
    val reg = VimPlugin.getRegister().currentRegister

    state.setCommand(lastCommand)
    state.isDotRepeatInProgress = true
    VimPlugin.getRegister().selectRegister(VimRepeater.lastChangeRegister)

    try {
      KeyHandler.executeVimAction(editor, lastCommand.action, context)
    } catch (ignored: Exception) {
    }

    state.isDotRepeatInProgress = false

    // Restore state
    if (save != null) state.setCommand(save)
    VimPlugin.getMotion().setLastFTCmd(lastFTCmd, lastFTChar)
    VimRepeater.saveLastChange(lastCommand)
    VimPlugin.getRegister().selectRegister(reg)
    return true
  }
}

object VimRepeater {
  var lastChangeCommand: Command? = null
  var lastChangeRegister = VimPlugin.getRegister().defaultRegister

  fun saveLastChange(command: Command) {
    lastChangeCommand = command
    lastChangeRegister = VimPlugin.getRegister().currentRegister
  }
}
