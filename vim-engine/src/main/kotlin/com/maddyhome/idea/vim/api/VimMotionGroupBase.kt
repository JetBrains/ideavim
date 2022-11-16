/*
 * Copyright 2003-2022 The IdeaVim authors
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
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.Motion.AbsoluteOffset
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.handler.toAdjustedMotionOrError
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.isEndAllowedIgnoringOnemore
import com.maddyhome.idea.vim.helper.mode
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

abstract class VimMotionGroupBase : VimMotionGroup {
  override var lastFTCmd = TillCharacterMotionType.LAST_SMALL_T
  override var lastFTChar: Char = ' '

  override fun getVerticalMotionOffset(editor: VimEditor, caret: VimCaret, count: Int): Motion {
    val pos = caret.getVisualPosition()
    if ((pos.line == 0 && count < 0) || (pos.line >= editor.getVisualLineCount() - 1 && count > 0)) {
      return Motion.Error
    }

    val intendedColumn = caret.vimLastColumn
    val line = editor.normalizeVisualLine(pos.line + count)

    if (intendedColumn == LAST_COLUMN) {
      val normalisedColumn = editor.normalizeVisualColumn(line, intendedColumn,
        editor.mode.isEndAllowedIgnoringOnemore
      )
      val newPos = VimVisualPosition(line, normalisedColumn, false)
      return editor.visualPositionToOffset(newPos).point.toAdjustedMotionOrError(intendedColumn)
    }

    if (line < 0) {
      // https://web.ea.pages.jetbrains.team/#/issue/266279
      // There is a weird exception for line < 0, but I don't understand how this may happen
      throw RuntimeException("Line is " + line + " , pos.line=" + pos.line + ", count=" + count)
    }

    val additionalVisualColumns = injector.engineEditorHelper
      .amountOfInlaysBeforeVisualPosition(editor, VimVisualPosition(line, intendedColumn, false))

    val normalisedColumn = editor
      .normalizeVisualColumn(line, intendedColumn, editor.isEndAllowed)
    val adjustedColumn = normalisedColumn + additionalVisualColumns

    val newPos = VimVisualPosition(line, adjustedColumn, false)
    val offset = editor.visualPositionToOffset(newPos).point
    return if (intendedColumn != adjustedColumn) {
      offset.toAdjustedMotionOrError(intendedColumn)
    }
    else {
      offset.toMotionOrError()
    }
  }

  override fun moveCaretToLineEnd(editor: VimEditor, line: Int, allowPastEnd: Boolean): Int {
    return editor.normalizeOffset(
      line,
      editor.getLineEndOffset(line, allowPastEnd),
      allowPastEnd
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
    val size = editor.fileSize().toInt()
    if ((searchFrom == 0 && count < 0) || (searchFrom >= size - 1 && count > 0)) {
      return Motion.Error
    }
    return (injector.searchHelper.findNextWord(editor, searchFrom, count, bigWord)).toMotionOrError()
  }

  override fun getOffsetOfHorizontalMotion(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    allowPastEnd: Boolean,
  ): Int {
    val oldOffset = caret.offset.point
    var diff = 0
    val text = editor.text()
    val sign = sign(count.toFloat()).toInt()
    for (pointer in IntProgression.fromClosedRange(0, count - sign, sign)) {
      val textPointer = oldOffset + pointer
      diff += if (textPointer < text.length && textPointer >= 0) {
        // Actual char size can differ from 1 if unicode characters are used (like 🐔)
        Character.charCount(Character.codePointAt(text, textPointer))
      } else {
        1
      }
    }
    val offset = editor
      .normalizeOffset(caret.getLine().line, oldOffset + (sign * diff), allowPastEnd)

    return if (offset == oldOffset) -1 else offset
  }

  override fun moveCaretToRelativeLineStartSkipLeading(
    editor: VimEditor,
    caret: VimCaret,
    linesOffset: Int,
  ): Int {
    val line = editor.normalizeVisualLine(caret.getVisualPosition().line + linesOffset)
    return moveCaretToLineStartSkipLeading(editor, editor.visualLineToLogicalLine(line))
  }

  override fun scrollFullPage(editor: VimEditor, caret: VimCaret, pages: Int): Boolean {
    assert(pages != 0)
    return if (pages > 0) scrollFullPageDown(editor, caret, pages) else scrollFullPageUp(editor, caret, abs(pages))
  }

  protected abstract fun scrollFullPageDown(editor: VimEditor, caret: VimCaret, pages: Int): Boolean
  protected abstract fun scrollFullPageUp(editor: VimEditor, caret: VimCaret, pages: Int): Boolean

  /**
   * This moves the caret next to the next/previous matching character on the current line
   *
   * @param caret  The caret to be moved
   * @param count  The number of occurrences to move to
   * @param ch     The character to search for
   * @param editor The editor to search in
   * @return True if [count] character matches were found, false if not
   */
  override fun moveCaretToBeforeNextCharacterOnLine(editor: VimEditor, caret: VimCaret, count: Int, ch: Char): Int {
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
  override fun moveCaretToNextCharacterOnLine(editor: VimEditor, caret: VimCaret, count: Int, ch: Char): Int {
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

  override fun moveCaretToCurrentLineStart(editor: VimEditor, caret: VimCaret): Int {
    val logicalLine = caret.getLine().line
    return moveCaretToLineStart(editor, logicalLine)
  }

  override fun moveCaretToRelativeLineEnd(
    editor: VimEditor,
    caret: VimCaret,
    cntForward: Int,
    allowPastEnd: Boolean,
  ): Int {
    val line = editor.normalizeVisualLine(caret.getVisualPosition().line + cntForward)

    return if (line < 0) 0 else {
      moveCaretToLineEnd(editor, editor.visualLineToLogicalLine(line), allowPastEnd)
    }
  }

  override fun moveCaretToRelativeLineEndSkipTrailing(editor: VimEditor, caret: VimCaret, linesOffset: Int): Int {
    val line = editor.visualLineToLogicalLine(
      editor.normalizeVisualLine(caret.getVisualPosition().line + linesOffset)
    )
    val start = editor.getLineStartOffset(line)
    val end = editor.getLineEndOffset(line, true)
    val chars = editor.text()
    var pos = start
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

  override fun repeatLastMatchChar(editor: VimEditor, caret: VimCaret, count: Int): Int {
    var res = -1
    val startPos = editor.currentCaret().offset.point
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

  override fun moveCaretToCurrentLineStartSkipLeading(editor: VimEditor, caret: VimCaret): Int {
    val logicalLine = caret.getLine().line
    return moveCaretToLineStartSkipLeading(editor, logicalLine)
  }

  override fun getMotionRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument,
    operatorArguments: OperatorArguments,
  ): TextRange? {
    var start: Int
    var end: Int
    if (argument.type === Argument.Type.OFFSETS) {
      val offsets = argument.offsets[caret] ?: return null
      val (first, second) = offsets.getNativeStartAndEnd()
      start = first
      end = second
    } else {
      val cmd = argument.motion
      // Normalize the counts between the command and the motion argument
      val cnt = cmd.count * operatorArguments.count1
      val raw = if (operatorArguments.count0 == 0 && cmd.rawCount == 0) 0 else cnt
      val cmdAction = cmd.action
      if (cmdAction is MotionActionHandler) {
        // This is where we are now
        start = caret.offset.point

        // Execute the motion (without moving the cursor) and get where we end
        val motion =
          cmdAction.getHandlerOffset(editor, caret, context, cmd.argument, operatorArguments.withCount0(raw))

        // Invalid motion
        if (Motion.Error == motion) return null
        if (Motion.NoMotion == motion) return null
        end = (motion as AbsoluteOffset).offset

        // If inclusive, add the last character to the range
        if (cmdAction.motionType === MotionType.INCLUSIVE && end < editor.fileSize()) {
          if (start > end) {
            start++
          } else {
            end++
          }
        }
      } else if (cmdAction is TextObjectActionHandler) {
        val range: TextRange = cmdAction.getRange(editor, caret, context, cnt, raw, cmd.argument)
          ?: return null
        start = range.startOffset
        end = range.endOffset
        if (cmd.isLinewiseMotion()) end--
      } else {
        throw RuntimeException(
          "Commands doesn't take " + cmdAction.javaClass.simpleName + " as an operator"
        )
      }

      // Normalize the range
      if (start > end) {
        val t = start
        start = end
        end = t
      }

      // If we are a linewise motion we need to normalize the start and stop then move the start to the beginning
      // of the line and move the end to the end of the line.
      if (cmd.isLinewiseMotion()) {
        if (caret.getLogicalPosition().line != editor.lineCount() - 1) {
          start = editor.lineStartForOffset(start)
          end = min((editor.lineEndForOffset(end) + 1).toLong(), editor.fileSize()).toInt()
        } else {
          start = editor.lineStartForOffset(start)
          end = editor.lineEndForOffset(end)
        }
      }
    }

    // This is a kludge for dw, dW, and d[w. Without this kludge, an extra newline is operated when it shouldn't be.
    val text = editor.text().subSequence(start, end).toString()
    val lastNewLine = text.lastIndexOf('\n')
    if (lastNewLine > 0) {
      val id = argument.motion.action.id
      if (id == "VimMotionWordRightAction" || id == "VimMotionBigWordRightAction" || id == "VimMotionCamelRightAction") {
        if (!injector.engineEditorHelper.anyNonWhitespace(editor, end, -1)) {
          end = start + lastNewLine
        }
      }
    }
    return TextRange(start, end)
  }

  override fun moveCaretToCurrentLineEnd(editor: VimEditor, caret: VimCaret): Int {
    val (line) = caret.getVisualPosition()
    val lastVisualLineColumn = editor.getLastVisualLineColumnNumber(line)
    val visualEndOfLine = VimVisualPosition(line, lastVisualLineColumn, true)
    return moveCaretToLineEnd(editor, editor.visualToLogicalPosition(visualEndOfLine).line, true)
  }

    override fun moveCaretToLineStartSkipLeading(editor: VimEditor, line: Int): Int {
        return editor.getLeadingCharacterOffset(line)
    }

    companion object {
        const val LAST_COLUMN = 9999
    }
}
