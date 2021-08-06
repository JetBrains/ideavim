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

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.extension.VimExtensionRegistrar
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext

/**
 * This handler is created to support `Plug` command from vim-plug and `Plugin` command from vundle.
 */
data class PlugCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)

  override fun processCommand(editor: Editor, context: DataContext, vimContext: VimContext): ExecutionResult {
    val argument = argument
    val firstChar = argument[0]
    if (firstChar != '"' && firstChar != '\'') return ExecutionResult.Error

    val pluginAlias = argument.drop(1).takeWhile { it != firstChar }
    val option = VimExtensionRegistrar.getToggleByAlias(pluginAlias) ?: return ExecutionResult.Error
    option.set()

    return ExecutionResult.Success
  }
}
