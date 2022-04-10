package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.TextRange

interface VimSearchGroup {
  var lastSearchPattern: String?
  var lastSubstitutePattern: String?
  fun findUnderCaret(editor: VimEditor): TextRange?
  fun searchBackward(editor: VimEditor, offset: Int, count: Int): TextRange?
  fun getNextSearchRange(editor: VimEditor, count: Int, forwards: Boolean): TextRange?
  fun processSearchRange(editor: VimEditor, pattern: String, patternOffset: Int, startOffset: Int, direction: Direction): Int
}