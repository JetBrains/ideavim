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
import com.maddyhome.idea.vim.ex.*
import com.maddyhome.idea.vim.group.copy.PutData
import com.maddyhome.idea.vim.helper.EditorHelper

class CopyTextHandler : CommandHandler.SingleExecution() {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.WRITABLE)
  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    val carets = EditorHelper.getOrderedCaretsList(editor)
    for (caret in carets) {
      val range = cmd.getTextRange(editor, caret, context, false)
      val text = EditorHelper.getText(editor, range.startOffset, range.endOffset)

      val arg = CommandParser.getInstance().parse(cmd.argument)
      val line = arg.ranges.getFirstLine(editor, caret, context)

      val transferableData = VimPlugin.getRegister().getTransferableData(editor, range, text)
      val textData = PutData.TextData(text, SelectionType.LINE_WISE, transferableData)
      val putData = PutData(textData, null, 1, insertTextBeforeCaret = false, _indent = true, caretAfterInsertedText = false, putToLine = line)
      VimPlugin.getPut().putTextForCaret(editor, caret, context, putData)
    }
    return true
  }
}
