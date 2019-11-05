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
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.flags
import com.maddyhome.idea.vim.group.copy.PutData

class PutLinesHandler : CommandHandler.SingleExecution() {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    if (editor.isOneLineMode) return false

    val registerGroup = VimPlugin.getRegister()
    val arg = cmd.argument
    if (arg.isNotEmpty() && !registerGroup.selectRegister(arg[0])) {
      return false
    } else {
      registerGroup.selectRegister(registerGroup.defaultRegister)
    }

    val line = if (cmd.ranges.size() == 0) -1 else cmd.getLine(editor)
    val textData = registerGroup.lastRegister?.let { PutData.TextData(it.text, SelectionType.LINE_WISE, it.transferableData) }
    val putData = PutData(textData, null, 1, insertTextBeforeCaret = false, _indent = false, caretAfterInsertedText = false, putToLine = line)
    return VimPlugin.getPut().putText(editor, context, putData)
  }
}
