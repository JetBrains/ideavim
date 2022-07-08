package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.action.motion.leftright.TillCharacterMotionType
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.isEndAllowedIgnoringOnemore
import com.maddyhome.idea.vim.helper.mode
import kotlin.math.abs
import kotlin.math.sign

abstract class VimMotionGroupBase : VimMotionGroup {
  override var lastFTCmd = TillCharacterMotionType.LAST_SMALL_T
  override var lastFTChar: Char = ' '
  override fun getVerticalMotionOffset(editor: VimEditor, caret: VimCaret, count: Int): Int {
    val pos = caret.getVisualPosition()
    if ((pos.line == 0 && count < 0) || (pos.line >= injector.engineEditorHelper.getVisualLineCount(editor) - 1 && count > 0)) {
      return -1
    } else {
      var col = caret.vimLastColumn
      val line = injector.engineEditorHelper.normalizeVisualLine(editor, pos.line + count)

      if (col == LAST_COLUMN) {
        col = injector.engineEditorHelper.normalizeVisualColumn(
          editor, line, col,
          editor.mode.isEndAllowedIgnoringOnemore
        )
      } else {
        if (line < 0) {
          // https://web.ea.pages.jetbrains.team/#/issue/266279
          // There is a weird exception for line < 0, but I don't understand how this may happen
          throw RuntimeException("Line is " + line + " , pos.line=" + pos.line + ", count=" + count)
        }
        val newInlineElements = injector.engineEditorHelper
          .amountOfInlaysBeforeVisualPosition(editor, VimVisualPosition(line, col, false))

        col = injector.engineEditorHelper
          .normalizeVisualColumn(editor, line, col, (editor).isEndAllowed)
        col += newInlineElements
      }

      val newPos = VimVisualPosition(line, col, false)
      return editor.visualPositionToOffset(newPos).point
    }
  }

  override fun moveCaretToLineEnd(editor: VimEditor, line: Int, allowPastEnd: Boolean): Int {
    return injector.engineEditorHelper.normalizeOffset(
      editor,
      line,
      injector.engineEditorHelper.getLineEndOffset(editor, line, allowPastEnd),
      allowPastEnd
    )
  }

  override fun moveCaretToLineStart(editor: VimEditor, line: Int): Int {
    if (line >= editor.lineCount()) {
      return editor.fileSize().toInt()
    }
    return injector.engineEditorHelper.getLineStartOffset(editor, line)
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
    val offset = injector.engineEditorHelper
      .normalizeOffset(editor, caret.getLine().line, oldOffset + (sign * diff), allowPastEnd)

    return if (offset == oldOffset) -1 else offset
  }

  override fun moveCaretToLineStartSkipLeadingOffset(
    editor: VimEditor,
    caret: VimCaret,
    linesOffset: Int,
  ): Int {
    val line = injector.engineEditorHelper.normalizeVisualLine(editor, caret.getVisualPosition().line + linesOffset)
    return moveCaretToLineStartSkipLeading(editor, injector.engineEditorHelper.visualLineToLogicalLine(editor, line))
  }

  override fun scrollFullPage(editor: VimEditor, caret: VimCaret, pages: Int): Boolean {
    assert(pages != 0)
    return if (pages > 0) scrollFullPageDown(editor, caret, pages) else scrollFullPageUp(editor, caret, abs(pages))
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

  override fun moveCaretToLineStart(editor: VimEditor, caret: VimCaret): Int {
    val logicalLine = caret.getLine().line
    return moveCaretToLineStart(editor, logicalLine)
  }

  override fun moveCaretToLineEndOffset(
    editor: VimEditor,
    caret: VimCaret,
    cntForward: Int,
    allowPastEnd: Boolean,
  ): Int {
    val line = injector.engineEditorHelper.normalizeVisualLine(editor, caret.getVisualPosition().line + cntForward)

    return if (line < 0) 0 else {
      moveCaretToLineEnd(editor, injector.engineEditorHelper.visualLineToLogicalLine(editor, line), allowPastEnd)
    }
  }

  override fun moveCaretToLineEndSkipLeadingOffset(editor: VimEditor, caret: VimCaret, linesOffset: Int): Int {
    val line = injector.engineEditorHelper.visualLineToLogicalLine(
      editor,
      injector.engineEditorHelper.normalizeVisualLine(editor, caret.getVisualPosition().line + linesOffset)
    )
    val start = injector.engineEditorHelper.getLineStartOffset(editor, line)
    val end = injector.engineEditorHelper.getLineEndOffset(editor, line, true)
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

  override fun moveCaretToLineStartSkipLeading(editor: VimEditor, caret: VimCaret): Int {
    val logicalLine = caret.getLine().line
    return moveCaretToLineStartSkipLeading(editor, logicalLine)
  }

  companion object {
    const val LAST_COLUMN = 9999
  }
}
