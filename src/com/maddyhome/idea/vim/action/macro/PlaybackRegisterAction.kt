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
package com.maddyhome.idea.vim.action.macro

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.ex.CommandParser
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.handler.VimActionHandler

class PlaybackRegisterAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override val argumentType: Argument.Type = Argument.Type.CHARACTER

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val argument = cmd.argument ?: return false
    val reg = argument.character
    val project = PlatformDataKeys.PROJECT.getData(context)
    val application = ApplicationManager.getApplication()
    val res = Ref.create(false)
    when (reg) {
        '@' -> {
          application.runWriteAction { res.set(VimPlugin.getMacro().playbackLastRegister(editor, context, project, cmd.count)) }
        }
        ':' -> { // No write action
          try {
            res.set(CommandParser.getInstance().processLastCommand(editor, context, cmd.count))
          } catch (e: ExException) {
            res.set(false)
          }
        }
        else -> {
          application.runWriteAction { res.set(VimPlugin.getMacro().playbackRegister(editor, context, project, reg, cmd.count)) }
        }
    }
    return res.get()
  }
}
