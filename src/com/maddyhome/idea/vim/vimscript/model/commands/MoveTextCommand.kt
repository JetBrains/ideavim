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
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.InvalidRangeException
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.group.copy.PutData
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser
import kotlin.math.min

/**
 * see "h :move"
 */
data class MoveTextCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.WRITABLE)

  @Throws(ExException::class)
  override fun processCommand(editor: Editor, context: DataContext, vimContext: VimContext): ExecutionResult {
    val carets = EditorHelper.getOrderedCaretsList(editor)
    val caretModel = editor.caretModel
    val caretCount = caretModel.caretCount

    val texts = ArrayList<String>(caretCount)
    val ranges = ArrayList<TextRange>(caretCount)
    var line = editor.fileSize
    val goToLineCommand = VimscriptParser.parseCommand(argument) ?: throw ExException("E16: Invalid range")

    var lastRange: TextRange? = null
    for (caret in carets) {
      val range = getTextRange(editor, caret, false)
      val lineRange = getLineRange(editor, caret)

      line = min(line, normalizeLine(editor, caret, goToLineCommand, lineRange))
      texts.add(EditorHelper.getText(editor, range.startOffset, range.endOffset))

      if (lastRange == null || lastRange.startOffset != range.startOffset && lastRange.endOffset != range.endOffset) {
        ranges.add(range)
        lastRange = range
      }
    }

    ranges.forEach { editor.document.deleteString(it.startOffset, it.endOffset) }

    for (i in 0 until caretCount) {
      val caret = carets[i]
      val text = texts[i]

      val textData = PutData.TextData(text, SelectionType.LINE_WISE, emptyList())
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

  @Throws
  private fun normalizeLine(
    editor: Editor,
    caret: Caret,
    command: Command,
    lineRange: LineRange,
  ): Int {
    var line = command.commandRanges.getFirstLine(editor, caret)
    val adj = lineRange.endLine - lineRange.startLine + 1
    if (line >= lineRange.endLine)
      line -= adj
    else if (line >= lineRange.startLine) throw InvalidRangeException(MessageHelper.message(Msg.e_backrange))

    return line
  }
}
