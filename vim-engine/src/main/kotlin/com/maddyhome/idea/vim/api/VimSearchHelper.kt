package com.maddyhome.idea.vim.api

interface VimSearchHelper {
  fun findNextParagraph(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    allowBlanks: Boolean,
  ): Int

  fun findNextSentenceStart(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): Int

  fun findSection(
    editor: VimEditor,
    caret: VimCaret,
    type: Char,
    dir: Int,
    count: Int,
  ): Int

  fun findNextCamelEnd(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
  ): Int
}