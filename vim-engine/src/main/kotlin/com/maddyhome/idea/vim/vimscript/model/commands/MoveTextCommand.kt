/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.InvalidRangeException
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.put.PutData
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import kotlin.math.min

/**
 * see "h :move"
 */
data class MoveTextCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_REQUIRED, Access.WRITABLE)

  @Throws(ExException::class)
  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    val carets = editor.sortedCarets()
    val caretCount = editor.nativeCarets().size

    val texts = ArrayList<String>(caretCount)
    val ranges = ArrayList<TextRange>(caretCount)
    var line = editor.fileSize().toInt()
    val goToLineCommand = injector.vimscriptParser.parseCommand(argument) ?: throw ExException("E16: Invalid range")

    var lastRange: TextRange? = null
    for (caret in carets) {
      val range = getTextRange(editor, caret, false)
      val lineRange = getLineRange(editor, caret)

      line = min(line, normalizeLine(editor, caret, goToLineCommand, lineRange))
      texts.add(editor.getText(range))

      if (lastRange == null || lastRange.startOffset != range.startOffset && lastRange.endOffset != range.endOffset) {
        ranges.add(range)
        lastRange = range
      }
    }

    ranges.forEach { editor.deleteString(it) }

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
      injector.put.putTextForCaret(editor, caret, context, putData)
    }

    return ExecutionResult.Success
  }

  @Throws
  private fun normalizeLine(
    editor: VimEditor,
    caret: VimCaret,
    command: Command,
    lineRange: LineRange,
  ): Int {
    var line = command.commandRanges.getFirstLine(editor, caret)
    val adj = lineRange.endLine - lineRange.startLine + 1
    if (line >= lineRange.endLine)
      line -= adj
    else if (line >= lineRange.startLine) throw InvalidRangeException(injector.messages.message(Msg.e_backrange))

    return line
  }
}
