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
}