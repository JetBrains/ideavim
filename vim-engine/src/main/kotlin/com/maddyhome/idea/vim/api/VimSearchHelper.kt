package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.SearchOptions
import java.util.*

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

  fun findNextWordEnd(
    chars: CharSequence,
    pos: Int,
    size: Int,
    count: Int,
    bigWord: Boolean,
    spaceWords: Boolean,
  ): Int

  fun findNextWord(editor: VimEditor, searchFrom: Int, count: Int, bigWord: Boolean): Long

  fun findPattern(
    editor: VimEditor,
    pattern: String?,
    startOffset: Int,
    count: Int,
    searchOptions: EnumSet<SearchOptions>?,
  ): TextRange?

  fun findNextCharacterOnLine(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    ch: Char,
  ): Int

  fun findWordUnderCursor(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    dir: Int,
    isOuter: Boolean,
    isBig: Boolean,
    hasSelection: Boolean,
  ): TextRange

  fun findSentenceRange(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    isOuter: Boolean,
  ): TextRange

  fun findParagraphRange(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    isOuter: Boolean,
  ): TextRange?

  fun findBlockTagRange(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    isOuter: Boolean,
  ): TextRange?

  fun findBlockQuoteInLineRange(
    editor: VimEditor,
    caret: VimCaret,
    quote: Char,
    isOuter: Boolean,
  ): TextRange?

  fun findBlockRange(
    editor: VimEditor,
    caret: VimCaret,
    type: Char,
    count: Int,
    isOuter: Boolean,
  ): TextRange?
}
