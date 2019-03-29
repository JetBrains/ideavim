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

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.*
import com.maddyhome.idea.vim.ex.CommandHandler.Flag.WRITABLE
import com.maddyhome.idea.vim.group.MarkGroup
import com.maddyhome.idea.vim.handler.CaretOrder
import com.maddyhome.idea.vim.helper.EditorHelper

class PutLinesHandler : CommandHandler(
        commands("pu[t]"),
        flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, WRITABLE)
) {

  @Throws(ExException::class)
  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    if (editor.isOneLineMode) return false

    val registerGroup = VimPlugin.getRegister()
    val arg = cmd.argument
    if (arg.isNotEmpty() && !registerGroup.selectRegister(arg[0])) {
      return false
    } else {
      registerGroup.selectRegister(registerGroup.defaultRegister)
    }

    val register = registerGroup.lastRegister ?: return false
    val text = register.text

    val lines = cmd.getOrderedLines(editor, context, CaretOrder.DECREASING_OFFSET)
    val carets = EditorHelper.getOrderedCaretsList(editor, CaretOrder.DECREASING_OFFSET)
    for (i in carets.indices) {
      val caret = carets[i]
      val line = lines[i]

      var startOffset = minOf(editor.document.textLength,
              VimPlugin.getMotion().moveCaretToLineEnd(editor, line, true) + 1)
      if (startOffset > 0 && startOffset == editor.document.textLength &&
              editor.document.charsSequence[startOffset - 1] != '\n') {
        editor.document.insertString(startOffset, "\n")
        startOffset++
      }

      if (text == null) {
        VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, startOffset)
        VimPlugin.getMark().setChangeMarks(editor, TextRange(startOffset, startOffset))
        continue
      }

      VimPlugin.getPut().putText(editor, caret, context, text, SelectionType.LINE_WISE, CommandState.SubMode.NONE,
              startOffset, 1, false, false)
    }

    return true
  }
}
