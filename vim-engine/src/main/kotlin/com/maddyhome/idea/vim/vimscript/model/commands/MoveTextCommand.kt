/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.SelectionInfo
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMarkService
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
import com.maddyhome.idea.vim.mark.Mark
import com.maddyhome.idea.vim.mark.VimMark
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
    val caretCount = editor.nativeCarets().size
    if (caretCount > 1) {
      throw ExException("Move command supported only for one caret at the moment")
    }
    val caret = editor.primaryCaret()
    val caretPosition = caret.getBufferPosition()

    val goToLineCommand = injector.vimscriptParser.parseCommand(argument) ?: throw ExException("E16: Invalid range")
    val lineRange = getLineRange(editor, caret)

    val line = min(editor.fileSize().toInt(), normalizeLine(editor, caret, goToLineCommand, lineRange))
    val range = getTextRange(editor, caret, false)
    val shift = line + 1 - editor.offsetToBufferPosition(range.startOffset).line

    val text = editor.getText(range)

    val localMarks = injector.markService.getAllLocalMarks(caret)
      .filter { range.contains(it.offset(editor)) }
      .filter { it.key != VimMarkService.SELECTION_START_MARK && it.key != VimMarkService.SELECTION_END_MARK }
      .toSet()
    val globalMarks = injector.markService.getGlobalMarks(editor)
      .filter { range.contains(it.offset(editor)) }
      .map { Pair(it, it.line) } // we save logical line because it will be cleared by Platform after text deletion
      .toSet()
    val lastSelectionInfo = caret.lastSelectionInfo
    val selectionStartBufferPosition = lastSelectionInfo.startOffset?.let { editor.offsetToBufferPosition(it) }
    val selectionEndBufferPosition = lastSelectionInfo.endOffset?.let { editor.offsetToBufferPosition(it) }

    editor.deleteString(range)

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

    globalMarks.forEach { shiftGlobalMark(editor, it, shift) }
    localMarks.forEach { shiftLocalMark(caret, it, shift) }
    shiftSelectionInfo(caret, selectionStartBufferPosition, selectionEndBufferPosition, lastSelectionInfo, shift, range)

    val newCaretPosition = shiftBufferPosition(caretPosition, shift)
    caret.moveToBufferPosition(newCaretPosition)

    return ExecutionResult.Success
  }

  private fun shiftGlobalMark(editor: VimEditor, markAndLine: Pair<Mark, Int>, shift: Int) {
    val newOffset = editor.bufferPositionToOffset(BufferPosition(markAndLine.second + shift, markAndLine.first.col))
    injector.markService.setGlobalMark(editor, markAndLine.first.key, newOffset)
  }

  private fun shiftLocalMark(caret: VimCaret, mark: Mark, shift: Int) {
    val editor = caret.editor
    val path = editor.getPath() ?: return
    val mark = VimMark(mark.key, mark.line + shift, mark.col, path, editor.extractProtocol())
    injector.markService.setMark(caret, mark)
  }

  private fun shiftSelectionInfo(
    caret: ImmutableVimCaret,
    startBufferPosition: BufferPosition?,
    endBufferPosition: BufferPosition?,
    selectionInfo: SelectionInfo,
    shift: Int,
    range: TextRange,
  ) {
    var newStartOffset = selectionInfo.startOffset
    var newEndOffset = selectionInfo.endOffset
    val editor = caret.editor

    if (selectionInfo.startOffset != null && startBufferPosition != null && range.contains(selectionInfo.startOffset)) {
      val newBufferPosition = shiftBufferPosition(startBufferPosition, shift)
      newStartOffset = editor.bufferPositionToOffset(newBufferPosition)
    }
    if (selectionInfo.endOffset != null && endBufferPosition != null && range.contains(selectionInfo.endOffset)) {
      val newBufferPosition = shiftBufferPosition(endBufferPosition, shift)
      newEndOffset = editor.bufferPositionToOffset(newBufferPosition)
    }

    if (newStartOffset != selectionInfo.startOffset || newEndOffset != selectionInfo.endOffset) {
      caret.lastSelectionInfo = SelectionInfo(newStartOffset, newEndOffset, selectionInfo.type)
    }
  }

  private fun shiftBufferPosition(bufferPosition: BufferPosition, shift: Int): BufferPosition {
    return BufferPosition(bufferPosition.line + shift, bufferPosition.column, bufferPosition.leansForward)
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
