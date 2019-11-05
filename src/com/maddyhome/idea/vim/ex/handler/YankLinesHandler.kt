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
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.*

class YankLinesHandler : CommandHandler.SingleExecution() {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  @Throws(ExException::class)
  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    val argument = cmd.argument
    val registerGroup = VimPlugin.getRegister()
    val register = if (argument.isNotEmpty() && !argument[0].isDigit()) {
      cmd.argument = argument.substring(1)
      argument[0]
    } else {
      registerGroup.defaultRegister
    }

    if (!registerGroup.selectRegister(register)) return false

    val caretModel = editor.caretModel
    val starts = ArrayList<Int>(caretModel.caretCount)
    val ends = ArrayList<Int>(caretModel.caretCount)
    for (caret in caretModel.allCarets) {
      val range = cmd.getTextRange(editor, caret, context, true)
      starts.add(range.startOffset)
      ends.add(range.endOffset)
    }

    return VimPlugin.getYank().yankRange(editor,
      TextRange(starts.toIntArray(), ends.toIntArray()),
      SelectionType.LINE_WISE, false)
  }
}
