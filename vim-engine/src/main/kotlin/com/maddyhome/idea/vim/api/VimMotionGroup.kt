package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.handler.Motion

interface VimMotionGroup {
  fun getVerticalMotionOffset(editor: VimEditor, caret: VimCaret, count: Int): Int
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
    allowPastEnd: Boolean
  ): Int

  fun moveCaretToLineStartSkipLeading(editor: VimEditor, line: Int): Int
  fun moveCaretToLineStartSkipLeadingOffset(
    editor: VimEditor,
    caret: VimCaret,
    linesOffset: Int
  ): Int

  fun scrollFullPageDown(editor: VimEditor, caret: VimCaret, pages: Int): Boolean
  fun scrollFullPageUp(editor: VimEditor, caret: VimCaret, pages: Int): Boolean
  fun scrollFullPage(editor: VimEditor, caret: VimCaret, pages: Int): Boolean
  fun moveCaretToMatchingPair(editor: VimEditor, caret: VimCaret): Int
  fun moveCaretToLinePercent(editor: VimEditor, caret: VimCaret, count: Int): Int
  fun moveCaretToLineWithStartOfLineOption(editor: VimEditor, logicalLine: Int, caret: VimCaret): Int
}