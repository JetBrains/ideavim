package com.maddyhome.idea.vim.api

interface VimMotionGroup {
  fun getVerticalMotionOffset(editor: VimEditor, caret: VimCaret, count: Int): Int
}