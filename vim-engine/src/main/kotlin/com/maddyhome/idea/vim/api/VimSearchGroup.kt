package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.vimscript.model.VimLContext

interface VimSearchGroup {
  var lastSearchPattern: String?
  var lastSubstitutePattern: String?
  fun findUnderCaret(editor: VimEditor): TextRange?
  fun searchBackward(editor: VimEditor, offset: Int, count: Int): TextRange?
  fun getNextSearchRange(editor: VimEditor, count: Int, forwards: Boolean): TextRange?
  fun processSearchRange(
    editor: VimEditor,
    pattern: String,
    patternOffset: Int,
    startOffset: Int,
    direction: Direction,
  ): Int

  fun searchNext(editor: VimEditor, caret: VimCaret, count: Int): Int
  fun searchPrevious(editor: VimEditor, caret: VimCaret, count: Int): Int
  fun processSearchCommand(editor: VimEditor, command: String, startOffset: Int, dir: Direction): Int
  fun searchWord(editor: VimEditor, caret: VimCaret, count: Int, whole: Boolean, dir: Direction): Int
  fun processSubstituteCommand(
    editor: VimEditor,
    caret: VimCaret,
    range: LineRange,
    excmd: String,
    exarg: String,
    parent: VimLContext,
  ): Boolean
}