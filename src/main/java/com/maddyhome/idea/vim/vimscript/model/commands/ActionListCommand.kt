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

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * @author smartbomb
 */
data class ActionListCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges) {
  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: Editor, context: DataContext): ExecutionResult {
    val lineSeparator = "\n"
    val searchPattern = argument.trim().toLowerCase().split("*")
    val actionManager = ActionManager.getInstance()

    // [VERSION UPDATE] 203+
    @Suppress("DEPRECATION")
    val actions = actionManager.getActionIds("")
      .sortedWith(String.CASE_INSENSITIVE_ORDER)
      .map { actionName ->
        val shortcuts = actionManager.getAction(actionName).shortcutSet.shortcuts.joinToString(" ") {
          if (it is KeyboardShortcut) StringHelper.toKeyNotation(it.firstKeyStroke) else it.toString()
        }
        if (shortcuts.isBlank()) actionName else "${actionName.padEnd(50)} $shortcuts"
      }
      .filter { line -> searchPattern.all { it in line.toLowerCase() } }
      .joinToString(lineSeparator)

    ExOutputModel.getInstance(editor).output(MessageHelper.message("ex.show.all.actions.0.1", lineSeparator, actions))
    return ExecutionResult.Success
  }
}
