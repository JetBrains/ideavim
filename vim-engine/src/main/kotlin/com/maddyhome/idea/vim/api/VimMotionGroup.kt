package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.action.motion.leftright.TillCharacterMotionType
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.handler.Motion

interface VimMotionGroup {
  var lastFTCmd: TillCharacterMotionType
  var lastFTChar: Char

  fun getVerticalMotionOffset(editor: VimEditor, caret: VimCaret, count: Int): Int
  fun moveCaretToLineEnd(editor: VimEditor, caret: VimCaret): Int
  fun moveCaretToLineEnd(editor: VimEditor, line: Int, allowPastEnd: Boolean): Int
  fun moveCaretToLineStart(editor: VimEditor, line: Int): Int

  /**
   * This moves the caret to the start of the next/previous word/WORD.
   *
   * @param editor  The editor to move in
   * @param count   The number of words to skip
   * @param bigWord If true then find WORD, if false then find word
   * @return position
   */
  fun findOffsetOfNextWord(editor: VimEditor, searchFrom: Int, count: Int, bigWord: Boolean): Motion
  fun getOffsetOfHorizontalMotion(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    allowPastEnd: Boolean,
  ): Int

  fun moveCaretToLineStartSkipLeading(editor: VimEditor, line: Int): Int
  fun moveCaretToLineStartSkipLeadingOffset(
    editor: VimEditor,
    caret: VimCaret,
    linesOffset: Int,
  ): Int

  fun scrollCaretIntoView(editor: VimEditor)
  fun scrollFullPageDown(editor: VimEditor, caret: VimCaret, pages: Int): Boolean
  fun scrollFullPageUp(editor: VimEditor, caret: VimCaret, pages: Int): Boolean
  fun scrollFullPage(editor: VimEditor, caret: VimCaret, pages: Int): Boolean
  fun moveCaretToMatchingPair(editor: VimEditor, caret: VimCaret): Int
  fun moveCaretToLinePercent(editor: VimEditor, caret: VimCaret, count: Int): Int
  fun moveCaretToLineWithStartOfLineOption(editor: VimEditor, logicalLine: Int, caret: VimCaret): Int

  /**
   * This moves the caret next to the next/previous matching character on the current line
   *
   * @param caret  The caret to be moved
   * @param count  The number of occurrences to move to
   * @param ch     The character to search for
   * @param editor The editor to search in
   * @return True if [count] character matches were found, false if not
   */
  fun moveCaretToBeforeNextCharacterOnLine(editor: VimEditor, caret: VimCaret, count: Int, ch: Char): Int

  /**
   * This moves the caret to the next/previous matching character on the current line
   *
   * @param caret  The caret to be moved
   * @param count  The number of occurrences to move to
   * @param ch     The character to search for
   * @param editor The editor to search in
   * @return True if [count] character matches were found, false if not
   */
  fun moveCaretToNextCharacterOnLine(editor: VimEditor, caret: VimCaret, count: Int, ch: Char): Int
  fun setLastFTCmd(lastFTCmd: TillCharacterMotionType, lastChar: Char)
  fun moveCaretToLineStart(
    editor: VimEditor,
    caret: VimCaret,
  ): Int

  fun moveCaretToLineEndOffset(
    editor: VimEditor,
    caret: VimCaret,
    cntForward: Int,
    allowPastEnd: Boolean,
  ): Int

  fun moveCaretToMiddleColumn(editor: VimEditor, caret: VimCaret): Motion
  fun moveCaretToLineScreenEnd(editor: VimEditor, caret: VimCaret, allowEnd: Boolean): Motion
  fun moveCaretToLineEndSkipLeadingOffset(editor: VimEditor, caret: VimCaret, linesOffset: Int): Int
  fun repeatLastMatchChar(editor: VimEditor, caret: VimCaret, count: Int): Int
  fun moveCaretToLineScreenStartSkipLeading(editor: VimEditor, caret: VimCaret): Int
  fun moveCaretToLineScreenStart(editor: VimEditor, caret: VimCaret): Motion
  fun moveCaretToLineStartSkipLeading(editor: VimEditor, caret: VimCaret): Int
  fun moveCaretToColumn(editor: VimEditor, caret: VimCaret, count: Int, allowEnd: Boolean): Motion
  fun scrollLineToMiddleScreenLine(editor: VimEditor, rawCount: Int, start: Boolean): Boolean
  fun scrollLine(editor: VimEditor, lines: Int): Boolean
  fun scrollLineToLastScreenLine(editor: VimEditor, rawCount: Int, start: Boolean): Boolean
  fun scrollCaretColumnToLastScreenColumn(editor: VimEditor): Boolean
  fun scrollColumns(editor: VimEditor, columns: Int): Boolean
  fun scrollScreen(editor: VimEditor, caret: VimCaret, rawCount: Int, down: Boolean): Boolean
  fun moveCaret(editor: VimEditor, caret: VimCaret, offset: Int)
  fun getMotionRange(editor: VimEditor, caret: VimCaret, context: ExecutionContext, argument: Argument, operatorArguments: OperatorArguments): TextRange?
  fun moveCaretToLineWithSameColumn(editor: VimEditor, logicalLine: Int, caret: VimCaret): Int
  fun scrollLineToFirstScreenLine(editor: VimEditor, rawCount: Int, start: Boolean): Boolean
  fun scrollCaretColumnToFirstScreenColumn(vimEditor: VimEditor): Boolean
  fun moveCaretToFirstScreenLine(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    normalizeToScreen: Boolean
  ): Int

  fun moveCaretToLastScreenLine(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    normalizeToScreen: Boolean
  ): Int

  fun moveCaretToMiddleScreenLine(editor: VimEditor, caret: VimCaret): Int
  fun moveCaretToFileMark(editor: VimEditor, ch: Char, toLineStart: Boolean): Int
  fun moveCaretToMark(editor: VimEditor, ch: Char, toLineStart: Boolean): Int
  fun moveCaretToJump(editor: VimEditor, count: Int): Int
  fun moveCaretGotoNextTab(editor: VimEditor, context: ExecutionContext, rawCount: Int): Int
  fun moveCaretGotoPreviousTab(editor: VimEditor, context: ExecutionContext, rawCount: Int): Int
}
