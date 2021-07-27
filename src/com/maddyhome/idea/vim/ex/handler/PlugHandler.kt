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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.CommandHandler.Access.READ_ONLY
import com.maddyhome.idea.vim.ex.CommandHandler.ArgumentFlag.ARGUMENT_REQUIRED
import com.maddyhome.idea.vim.ex.CommandHandler.RangeFlag.RANGE_FORBIDDEN
import com.maddyhome.idea.vim.ex.CommandHandlerFlags
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.ex.vimscript.VimScriptCommandHandler
import com.maddyhome.idea.vim.extension.VimExtensionRegistrar

/**
 * This handler is created to support `Plug` command from vim-plug and `Plugin` command from vundle.
 */
class PlugHandler : CommandHandler.SingleExecution(), VimScriptCommandHandler {
  override val argFlags: CommandHandlerFlags = flags(RANGE_FORBIDDEN, ARGUMENT_REQUIRED, READ_ONLY)

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean = doExecute(cmd)

  override fun execute(cmd: ExCommand) {
    doExecute(cmd)
  }

  private fun doExecute(cmd: ExCommand): Boolean {
    val argument = cmd.argument
    val firstChar = argument[0]
    if (firstChar != '"' && firstChar != '\'') return false

    val pluginAlias = argument.drop(1).takeWhile { it != firstChar }
    val option = VimExtensionRegistrar.getToggleByAlias(pluginAlias) ?: return false
    option.set()

    return true
  }
}
