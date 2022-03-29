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

  fun findNextSentenceEnd(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): Int

  fun findNextCamelStart(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
  ): Int

  fun findMethodEnd(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
  ): Int


  fun findMethodStart(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
  ): Int

  fun findUnmatchedBlock(
    editor: VimEditor,
    caret: VimCaret,
    type: Char,
    count: Int,
  ): Int

  fun findNextWordEnd(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    bigWord: Boolean,
  ): Int
}