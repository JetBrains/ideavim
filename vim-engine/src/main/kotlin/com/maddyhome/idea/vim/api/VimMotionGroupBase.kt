/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.action.motion.leftright.TillCharacterMotionType
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.Graphemes
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.findMatchingPairOnCurrentLine
import com.maddyhome.idea.vim.handler.ExternalActionHandler
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.Motion.AbsoluteOffset
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.handler.toAdjustedMotionOrError
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.state.mode.isEndAllowedIgnoringOnemore
import org.jetbrains.annotations.Range
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.min

abstract class VimMotionGroupBase : VimMotionGroup {
  override var lastFTCmd: TillCharacterMotionType = TillCharacterMotionType.LAST_SMALL_T
  override var lastFTChar: Char = ' '

  override fun getVerticalMotionOffset(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Motion {
    val pos = caret.getVisualPosition()
    if ((pos.line == 0 && count < 0) || (pos.line >= editor.getVisualLineCount() - 1 && count > 0)) {
      return Motion.Error
    }

    val intendedColumn = caret.vimLastColumn
    val line = editor.normalizeVisualLine(pos.line + count)

    if (intendedColumn == LAST_COLUMN) {
      val normalisedColumn = injector.engineEditorHelper.normalizeVisualColumn(
        editor,
        line,
        intendedColumn,
        editor.mode.isEndAllowedIgnoringOnemore,
      )
      val newPos = VimVisualPosition(line, normalisedColumn, false)
      val newOffset = editor.visualPositionToOffset(newPos)
      val adjustedOffset = editor.normalizeOffset(newOffset, editor.isEndAllowed)
      return adjustedOffset.toAdjustedMotionOrError(intendedColumn)
    }

    if (line < 0) {
      // https://web.ea.pages.jetbrains.team/#/issue/266279
      // There is a weird exception for line < 0, but I don't understand how this may happen
      throw RuntimeException("Line is " + line + " , pos.line=" + pos.line + ", count=" + count)
    }

    val additionalVisualColumns = injector.engineEditorHelper
      .amountOfInlaysBeforeVisualPosition(editor, VimVisualPosition(line, intendedColumn, false))

    val newPos = VimVisualPosition(line, intendedColumn + additionalVisualColumns, false)
    val offset = editor.visualPositionToOffset(newPos)
    val finalColumn = editor.offsetToVisualPosition(offset).column
    val bufferLine = editor.offsetToBufferPosition(offset).line
    val normalisedOffset = editor.normalizeOffset(bufferLine, offset, editor.isEndAllowed)
    return if (intendedColumn != finalColumn) {
      normalisedOffset.toAdjustedMotionOrError(intendedColumn)
    } else {
      normalisedOffset.toMotionOrError()
    }
  }

  override fun moveCaretToLineEnd(editor: VimEditor, line: Int, allowPastEnd: Boolean): Int {
    return editor.normalizeOffset(
      line,
      editor.getLineEndOffset(line, allowPastEnd),
      allowPastEnd,
    )
  }

  override fun moveCaretToLineStart(editor: VimEditor, line: Int): Int {
    if (line >= editor.lineCount()) {
      return editor.fileSize().toInt()
    }
    return editor.getLineStartOffset(line)
  }

  /**
   * This moves the caret to the start of the next/previous word/WORD.
   *
   * @param editor  The editor to move in
   * @param count   The number of words to skip
   * @param bigWord If true then find WORD, if false then find word
   * @return position
   */
  override fun findOffsetOfNextWord(editor: VimEditor, searchFrom: Int, count: Int, bigWord: Boolean): Motion {
    return findOffsetOfNextWord(editor.text(), searchFrom, count, bigWord, editor)
  }

  override fun findOffsetOfNextWord(
    text: CharSequence,
    searchFrom: Int,
    count: Int,
    bigWord: Boolean,
    editor: VimEditor,
  ): Motion {
    if ((searchFrom == 0 && count < 0) || (searchFrom >= text.length - 1 && count > 0)) {
      return Motion.Error
    }
    return (injector.searchHelper.findNextWord(text, editor, searchFrom, count, bigWord)).toMotionOrError()
  }

  override fun getHorizontalMotion(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    allowPastEnd: Boolean,
    allowWrap: Boolean,
  ): Motion {
    val text = editor.text()
    val oldOffset = caret.offset
    var current = oldOffset
    repeat(count.absoluteValue) {
      val newOffset = if (count > 0) Graphemes.next(text, current) else Graphemes.prev(text, current)
      current = newOffset ?: return@repeat
    }

    val offset = if (allowWrap) {
      var newOffset = current
      val oldLine = editor.offsetToBufferPosition(oldOffset).line
      val newLine = editor.offsetToBufferPosition(newOffset).line
      if (!allowPastEnd && count > 0 && oldLine == newLine && newOffset == editor.getLineEndForOffset(newOffset)) {
        ++newOffset // here we skip the /n char and move caret one char forward
      }
      editor.normalizeOffset(newOffset, allowPastEnd)
    } else {
      editor.normalizeOffset(caret.getLine(), current, allowPastEnd)
    }

    return offset.toMotionOrError()
  }

  override fun moveCaretToRelativeLineStartSkipLeading(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    linesOffset: Int,
  ): Int {
    val line = editor.normalizeVisualLine(caret.getVisualPosition().line + linesOffset)
    return moveCaretToLineStartSkipLeading(editor, editor.visualLineToBufferLine(line))
  }

  /**
   * This moves the caret next to the next/previous matching character on the current line
   *
   * @param caret  The caret to be moved
   * @param count  The number of occurrences to move to
   * @param ch     The character to search for
   * @param editor The editor to search in
   * @return True if [count] character matches were found, false if not
   */
  override fun moveCaretToBeforeNextCharacterOnLine(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    ch: Char,
  ): Int {
    val pos = injector.searchHelper.findNextCharacterOnLine(editor, caret, count, ch)

    return if (pos >= 0) {
      val step = if (count >= 0) 1 else -1
      pos - step
    } else {
      -1
    }
  }

  /**
   * This moves the caret to the next/previous matching character on the current line
   *
   * @param caret  The caret to be moved
   * @param count  The number of occurrences to move to
   * @param ch     The character to search for
   * @param editor The editor to search in
   * @return True if [count] character matches were found, false if not
   */
  override fun moveCaretToNextCharacterOnLine(editor: VimEditor, caret: ImmutableVimCaret, count: Int, ch: Char): Int {
    val pos = injector.searchHelper.findNextCharacterOnLine(editor, caret, count, ch)

    return if (pos >= 0) {
      pos
    } else {
      -1
    }
  }

  override fun setLastFTCmd(lastFTCmd: TillCharacterMotionType, lastChar: Char) {
    this.lastFTCmd = lastFTCmd
    this.lastFTChar = lastChar
  }

  override fun moveCaretToCurrentLineStart(editor: VimEditor, caret: ImmutableVimCaret): Int {
    val line = caret.getLine()
    return moveCaretToLineStart(editor, line)
  }

  override fun moveCaretToRelativeLineEnd(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    cntForward: Int,
    allowPastEnd: Boolean,
  ): Int {
    val line = editor.normalizeVisualLine(caret.getVisualPosition().line + cntForward)

    return if (line < 0) {
      0
    } else {
      moveCaretToLineEnd(editor, editor.visualLineToBufferLine(line), allowPastEnd)
    }
  }

  override fun moveCaretToRelativeLineEndSkipTrailing(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    linesOffset: Int,
  ): Int {
    val line = editor.visualLineToBufferLine(
      editor.normalizeVisualLine(caret.getVisualPosition().line + linesOffset),
    )
    return moveCaretToLineEndSkipTrailing(editor, line)
  }

  override fun repeatLastMatchChar(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int {
    var res = -1
    val startPos = editor.currentCaret().offset
    when (lastFTCmd) {
      TillCharacterMotionType.LAST_F -> res = moveCaretToNextCharacterOnLine(editor, caret, -count, lastFTChar)
      TillCharacterMotionType.LAST_SMALL_F ->
        res =
          moveCaretToNextCharacterOnLine(editor, caret, count, lastFTChar)

      TillCharacterMotionType.LAST_T -> {
        res = moveCaretToBeforeNextCharacterOnLine(editor, caret, -count, lastFTChar)
        if (res == startPos && abs(count) == 1) {
          res = moveCaretToBeforeNextCharacterOnLine(editor, caret, -2 * count, lastFTChar)
        }
      }

      TillCharacterMotionType.LAST_SMALL_T -> {
        res = moveCaretToBeforeNextCharacterOnLine(editor, caret, count, lastFTChar)
        if (res == startPos && abs(count) == 1) {
          res = moveCaretToBeforeNextCharacterOnLine(editor, caret, 2 * count, lastFTChar)
        }
      }
    }

    return res
  }

  override fun moveCaretToCurrentLineStartSkipLeading(editor: VimEditor, caret: ImmutableVimCaret): Int {
    val line = caret.getLine()
    return moveCaretToLineStartSkipLeading(editor, line)
  }

  override fun moveCaretToMark(caret: ImmutableVimCaret, ch: Char, toLineStart: Boolean): Motion {
    val markService = injector.markService
    val mark = markService.getMark(caret, ch) ?: return Motion.Error

    val editor = caret.editor

    val line = mark.line

    if (editor.getPath() == mark.filepath) {
      val offset = if (toLineStart) {
        moveCaretToLineStartSkipLeading(editor, line)
      } else {
        editor.bufferPositionToOffset(BufferPosition(line, mark.col, false))
      }
      return offset.toMotionOrError()
    }

    // TODO [vakhitov] It is super super super wrong.
    // TODO [vakhitov] We should remove all of the secondary carets and return an offset of the primary one
    val markEditor = injector.file.selectEditor(editor.projectId, mark.filepath, mark.protocol) ?: return Motion.Error
    // todo should we move all the carets or only one?
    for (carett in markEditor.carets()) {
      val offset = if (toLineStart) {
        moveCaretToLineStartSkipLeading(markEditor, line)
      } else {
        // todo should it be the same as getting offset above?
        markEditor.bufferPositionToOffset(BufferPosition(line, mark.col))
      }
      carett.moveToOffset(offset)
    }
    // TODO remove secondary carets and return result for primary caret
    return Motion.Error
  }

  override fun moveCaretToMarkRelative(caret: ImmutableVimCaret, count: Int): Motion {
    val markService = injector.markService
    val mark = markService.getRelativeLowercaseMark(caret, count) ?: return Motion.Error
    val offset = caret.editor.bufferPositionToOffset(BufferPosition(mark.line, mark.col, false))
    return offset.toMotionOrError()
  }

  override fun moveCaretToJump(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Motion {
    val jumpService = injector.jumpService
    val spot = jumpService.getJumpSpot(editor)
    val (line, col, fileName, protocol) = jumpService.getJump(editor, count) ?: return Motion.Error
    val lp = BufferPosition(line, col, false)
    return if (editor.getPath() != fileName) {
      // TODO [vakhitov] come up with a more gentle way to handle protocol
      injector.file.selectEditor(editor.projectId, fileName, protocol)?.let { newEditor ->
        if (spot == -1) {
          jumpService.addJump(editor, false)
        }
        newEditor.let {
          it.currentCaret().moveToOffset(it.normalizeOffset(newEditor.bufferPositionToOffset(lp), false))
        }
      }
      Motion.Error
    } else {
      if (spot == -1) {
        jumpService.addJump(editor, false)
      }
      editor.bufferPositionToOffset(lp).toMotionOrError()
    }
  }

  override fun getMotionRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments,
  ): TextRange? {
    if (argument !is Argument.Motion) {
      throw RuntimeException("Unexpected argument passed to getMotionRange2: $argument")
    }

    var start: Int
    var end: Int

    val action = argument.motion
    when (action) {
      is MotionActionHandler -> {
        // This is where we are now
        start = caret.offset

        // Execute the motion (without moving the cursor) and get where we end
        val motion = action.getHandlerOffset(editor, caret, context, argument.argument, operatorArguments)
        if (Motion.Error == motion || Motion.NoMotion == motion) return null

        end = (motion as AbsoluteOffset).offset

        // If inclusive, add the last character to the range
        if (action.motionType === MotionType.INCLUSIVE) {
          if (start > end) {
            if (start < editor.fileSize()) start++
          } else {
            if (end < editor.fileSize()) end++
          }
        }
      }

      is TextObjectActionHandler -> {
        val range: TextRange =
          action.getRange(editor, caret, context, operatorArguments.count1, operatorArguments.count0)
            ?: return null
        start = range.startOffset
        end = range.endOffset
        if (argument.isLinewiseMotion()) end--
      }

      is ExternalActionHandler -> {
        val range: TextRange = action.getRange(caret) ?: return null
        start = range.startOffset
        end = range.endOffset
        if (argument.isLinewiseMotion()) end--
      }

      else -> throw RuntimeException("Commands doesn't take " + action.javaClass.simpleName + " as an operator")
    }

    // Normalize the range
    if (start > end) {
      val t = start
      start = end
      end = t
    }

    // If we are a linewise motion we need to normalize the start and stop then move the start to the beginning
    // of the line and move the end to the end of the line.
    if (argument.isLinewiseMotion()) {
      start = editor.getLineStartForOffset(start)
      end = if (caret.getBufferPosition().line != editor.lineCount() - 1) {
        min((editor.getLineEndForOffset(end) + 1).toLong(), editor.fileSize()).toInt()
      } else {
        editor.getLineEndForOffset(end)
      }
    }

    // This is a kludge for dw, dW, and d[w. Without this kludge, an extra newline is operated when it shouldn't be.
    val text = editor.text().subSequence(start, end).toString()
    val lastNewLine = text.lastIndexOf('\n')
    if (lastNewLine > 0) {
      val id = action.id
      if (id == "VimMotionWordRightAction" || id == "VimMotionBigWordRightAction" || id == "VimMotionCamelRightAction") {
        if (!editor.anyNonWhitespace(end, -1)) {
          end = start + lastNewLine
        }
      }
    }

    return TextRange(start, end)
  }

  override fun moveCaretToColumn(editor: VimEditor, caret: ImmutableVimCaret, count: Int, allowEnd: Boolean): Motion {
    val line = caret.getLine()
    val column = editor.normalizeColumn(line, count, allowEnd)
    val offset = editor.bufferPositionToOffset(BufferPosition(line, column, false))
    return if (column != count) {
      Motion.AdjustedOffset(offset, count)
    } else {
      AbsoluteOffset(offset)
    }
  }

  override fun moveCaretToLineWithSameColumn(
    editor: VimEditor,
    line: Int,
    caret: ImmutableVimCaret,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    var c = caret.vimLastColumn
    var l = line
    if (l < 0) {
      l = 0
      c = 0
    } else if (l >= editor.lineCount()) {
      l = editor.normalizeLine(editor.lineCount() - 1)
      c = editor.lineLength(l)
    }
    val newPos = BufferPosition(l, editor.normalizeColumn(l, c, false))
    return editor.bufferPositionToOffset(newPos)
  }

  override fun moveCaretToLinePercent(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    return moveCaretToLineWithStartOfLineOption(
      editor,
      editor.normalizeLine(
        (editor.lineCount() * count.coerceIn(
          0,
          100
        ) + 99) / 100 - 1
      ), // TODO why do we have this 99? (It is there since 2003)
      caret,
    )
  }

  override fun moveCaretToMatchingPair(editor: VimEditor, caret: ImmutableVimCaret): Motion {
    return findMatchingPairOnCurrentLine(editor, caret)?.toMotionOrError() ?: Motion.Error
  }

  override fun moveCaretToLineWithStartOfLineOption(
    editor: VimEditor,
    line: Int,
    caret: ImmutableVimCaret,
  ): @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int {
    return if (injector.options(editor).startofline) {
      moveCaretToLineStartSkipLeading(editor, line)
    } else {
      moveCaretToLineWithSameColumn(editor, line, caret)
    }
  }

  override fun moveCaretToCurrentLineEnd(editor: VimEditor, caret: ImmutableVimCaret): Int {
    val (line) = caret.getVisualPosition()
    val lastVisualLineColumn = editor.getLastVisualLineColumnNumber(line)
    val visualEndOfLine = VimVisualPosition(line, lastVisualLineColumn, true)
    return moveCaretToLineEnd(editor, editor.visualPositionToBufferPosition(visualEndOfLine).line, true)
  }

  override fun moveCaretToLineStartSkipLeading(editor: VimEditor, line: Int): Int {
    return editor.getLeadingCharacterOffset(line)
  }

  override fun moveCaretToLineEndSkipTrailing(editor: VimEditor, line: Int): Int {
    val start = editor.getLineStartOffset(line)
    val end = editor.getLineEndOffset(line, true)
    val chars = editor.text()
    var pos = end
    for (offset in end downTo start + 1) {
      if (offset >= chars.length) {
        break
      }

      if (!Character.isWhitespace(chars[offset])) {
        pos = offset
        break
      }
    }

    return pos
  }

  companion object {
    const val LAST_COLUMN: Int = 9999
  }
}
