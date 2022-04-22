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

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser

/**
 * see "h :copy"
 */
data class CopyTextCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.WRITABLE)

  override fun processCommand(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    val carets = EditorHelper.getOrderedCaretsList(editor.ij).map { IjVimCaret(it) }
    for (caret in carets) {
      val range = getTextRange(editor, caret, false)
      val text = EditorHelper.getText(editor.ij, range.startOffset, range.endOffset)

      val goToLineCommand = VimscriptParser.parseCommand(argument) ?: throw ExException("E16: Invalid range")
      val line = goToLineCommand.commandRanges.getFirstLine(editor, caret)

      val transferableData = injector.clipboardManager.getTransferableData(editor, range, text)
      val textData = PutData.TextData(text, SelectionType.LINE_WISE, transferableData)
      val putData = PutData(
        textData,
        null,
        1,
        insertTextBeforeCaret = false,
        rawIndent = true,
        caretAfterInsertedText = false,
        putToLine = line
      )
      VimPlugin.getPut().putTextForCaret(editor, caret, context, putData)
    }
    return ExecutionResult.Success
  }
}
