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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.CommandHandler.Flag.ARGUMENT_OPTIONAL
import com.maddyhome.idea.vim.ex.CommandHandler.Flag.DONT_REOPEN
import com.maddyhome.idea.vim.ex.CommandHandler.Flag.RANGE_FORBIDDEN
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.ex.commands
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.helper.StringHelper

/**
 * @author smartbomb
 */
class ActionListHandler : CommandHandler(
        commands("actionlist"),
        flags(RANGE_FORBIDDEN, DONT_REOPEN, ARGUMENT_OPTIONAL)
) {
    override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
        val lineSeparator = System.lineSeparator()
        val searchPattern = cmd.argument.trim().toLowerCase().split("*")
        val actionManager = ActionManager.getInstance()

        val actions = actionManager.getActionIds("")
                .filter { actionName -> searchPattern.all { it in actionName.toLowerCase() } }
                .sortedWith(String.CASE_INSENSITIVE_ORDER).joinToString(lineSeparator) { actionName ->
                    val shortcuts = actionManager.getAction(actionName).shortcutSet.shortcuts.joinToString(" ") {
                        if (it is KeyboardShortcut) StringHelper.toKeyNotation(it.firstKeyStroke) else it.toString()
                    }
                    if (shortcuts.isBlank()) actionName else "${actionName.padEnd(50)} $shortcuts"
                }


        ExOutputModel.getInstance(editor).output("--- Actions ---$lineSeparator$actions")
        return true
    }
}
