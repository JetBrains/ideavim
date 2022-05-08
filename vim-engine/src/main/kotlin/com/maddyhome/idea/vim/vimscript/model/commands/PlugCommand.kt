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

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * This handler is created to support `Plug` command from vim-plug and `Plugin` command from vundle.
 */
data class PlugCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_REQUIRED, Access.READ_ONLY)

  override fun processCommand(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    val argument = argument
    val firstChar = argument[0]
    if (firstChar != '"' && firstChar != '\'') return ExecutionResult.Error

    val pluginAlias = argument.drop(1).takeWhile { it != firstChar }
    if (!injector.extensionRegistrator.setOptionByPluginAlias(pluginAlias)) {
      return ExecutionResult.Error
    }

    injector.statisticsService.addExtensionEnabledWithPlug(injector.extensionRegistrator.getExtensionNameByAlias(pluginAlias) ?: "unknown extension")
    return ExecutionResult.Success
  }
}
