package com.maddyhome.idea.vim.api

interface VimCaretListener {
  fun caretRemoved(caret: VimCaret?)
}
