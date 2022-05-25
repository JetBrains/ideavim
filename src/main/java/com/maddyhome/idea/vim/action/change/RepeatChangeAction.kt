/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

import com.intellij.openapi.command.CommandProcessor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.newapi.ij

class RepeatChangeAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_WRITABLE

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    val state = editor.commandState
    val lastCommand = VimRepeater.lastChangeCommand

    if (lastCommand == null && Extension.lastExtensionHandler == null) return false

    // Save state
    val save = state.executingCommand
    val lastFTCmd = injector.motion.lastFTCmd
    val lastFTChar = injector.motion.lastFTChar
    val reg = injector.registerGroup.currentRegister
    val lastHandler = Extension.lastExtensionHandler
    val repeatHandler = VimRepeater.repeatHandler

    state.isDotRepeatInProgress = true

    // A fancy 'redo-register' feature
    // VIM-2643, :h redo-register
    if (VimRepeater.lastChangeRegister in '1'..'8') {
      VimRepeater.lastChangeRegister = VimRepeater.lastChangeRegister.inc()
    }

    injector.registerGroup.selectRegister(VimRepeater.lastChangeRegister)

    try {
      if (repeatHandler && lastHandler != null) {
        val processor = CommandProcessor.getInstance()
        processor.executeCommand(
          editor.ij.project,
          { lastHandler.execute(editor, context) },
          "Vim " + lastHandler.javaClass.simpleName,
          null
        )
      } else if (!repeatHandler && lastCommand != null) {
        if (cmd.rawCount > 0) {
          lastCommand.count = cmd.count
          val arg = lastCommand.argument
          if (arg != null) {
            val mot = arg.motion
            mot.count = 0
          }
        }
        state.setExecutingCommand(lastCommand)

        val arguments = operatorArguments.copy(count0 = lastCommand.rawCount)
        injector.actionExecutor.executeVimAction(editor, lastCommand.action, context, arguments)

        VimRepeater.saveLastChange(lastCommand)
      }
    } catch (ignored: Exception) {
    }

    state.isDotRepeatInProgress = false

    // Restore state
    if (save != null) state.setExecutingCommand(save)
    VimPlugin.getMotion().setLastFTCmd(lastFTCmd, lastFTChar)
    if (lastHandler != null) Extension.lastExtensionHandler = lastHandler
    VimRepeater.repeatHandler = repeatHandler
    Extension.reset()
    VimPlugin.getRegister().selectRegister(reg)
    return true
  }
}
