package com.maddyhome.idea.vim.api

interface VimMotionGroup {
  fun getVerticalMotionOffset(editor: VimEditor, caret: VimCaret, count: Int): Int
    fun moveCaretToLineEnd(editor: VimEditor, line: Int, allowPastEnd: Boolean): Int
  fun moveCaretToLineStart(editor: VimEditor, line: Int): Int
}