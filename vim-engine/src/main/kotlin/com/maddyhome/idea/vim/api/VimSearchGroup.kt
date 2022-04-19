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
  fun searchNext(editor: VimEditor, caret: VimCaret, count: Int): Int
  fun searchPrevious(editor: VimEditor, caret: VimCaret, count: Int): Int
  fun processSearchCommand(editor: VimEditor, command: String, startOffset: Int, dir: Direction): Int
  fun searchWord(editor: VimEditor, caret: VimCaret, count: Int, whole: Boolean, dir: Direction): Int
}