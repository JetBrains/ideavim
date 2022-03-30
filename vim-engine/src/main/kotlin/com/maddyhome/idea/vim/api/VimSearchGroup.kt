package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange

interface VimSearchGroup {
  fun findUnderCaret(editor: VimEditor): TextRange?
  fun searchBackward(editor: VimEditor, offset: Int, count: Int): TextRange?
  fun getNextSearchRange(editor: VimEditor, count: Int, forwards: Boolean): TextRange?
}