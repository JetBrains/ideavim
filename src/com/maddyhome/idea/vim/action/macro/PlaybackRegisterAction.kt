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
package com.maddyhome.idea.vim.action.macro

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.group.RegisterGroup
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.vimscript.Executor

class PlaybackRegisterAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override val argumentType: Argument.Type = Argument.Type.CHARACTER

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val argument = cmd.argument ?: return false
    val reg = argument.character
    val project = PlatformDataKeys.PROJECT.getData(context)
    val application = ApplicationManager.getApplication()
    val res = Ref.create(false)
    when {
      reg == RegisterGroup.LAST_COMMAND_REGISTER || (reg == '@' && VimPlugin.getMacro().lastRegister == RegisterGroup.LAST_COMMAND_REGISTER) -> { // No write action
        try {
          var i = 0
          while (i < cmd.count) {
            res.set(Executor.executeLastCommand(editor, context))
            if (!res.get()) {
              break
            }
            i += 1
          }
          VimPlugin.getMacro().setLastRegister(reg)
        } catch (e: ExException) {
          res.set(false)
        }
      }
      reg == '@' -> {
        application.runWriteAction {
          res.set(
            VimPlugin.getMacro().playbackLastRegister(editor, context, project, cmd.count)
          )
        }
      }
      else -> {
        application.runWriteAction {
          res.set(
            VimPlugin.getMacro().playbackRegister(editor, context, project, reg, cmd.count)
          )
        }
      }
    }
    return res.get()
  }
}
