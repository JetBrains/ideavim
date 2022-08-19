package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimSearchHelperBase
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.SearchHelper
import com.maddyhome.idea.vim.helper.SearchOptions
import java.util.*

@Service
class IjVimSearchHelper : VimSearchHelperBase() {
  override fun findNextParagraph(editor: VimEditor, caret: VimCaret, count: Int, allowBlanks: Boolean): Int {
    return SearchHelper.findNextParagraph(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
      allowBlanks
    )
  }

  override fun findNextSentenceStart(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): Int {
    return SearchHelper.findNextSentenceStart(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count, countCurrent, requireAll
    )
  }

  override fun findSection(editor: VimEditor, caret: VimCaret, type: Char, dir: Int, count: Int): Int {
    return SearchHelper.findSection(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      type,
      dir,
      count,
    )
  }

  override fun findNextCamelEnd(editor: VimEditor, caret: VimCaret, count: Int): Int {
    return SearchHelper.findNextCamelEnd(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
    )
  }

  override fun findNextSentenceEnd(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): Int {
    return SearchHelper.findNextSentenceEnd(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
      countCurrent,
      requireAll,
    )
  }

  override fun findNextCamelStart(editor: VimEditor, caret: VimCaret, count: Int): Int {
    return SearchHelper.findNextCamelStart(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
    )
  }

  override fun findMethodEnd(editor: VimEditor, caret: VimCaret, count: Int): Int {
    return SearchHelper.findMethodEnd(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
    )
  }

  override fun findMethodStart(editor: VimEditor, caret: VimCaret, count: Int): Int {
    return SearchHelper.findMethodStart(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
    )
  }

  override fun findUnmatchedBlock(editor: VimEditor, caret: VimCaret, type: Char, count: Int): Int {
    return SearchHelper.findUnmatchedBlock(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      type,
      count,
    )
  }

  override fun findNextWordEnd(editor: VimEditor, caret: VimCaret, count: Int, bigWord: Boolean): Int {
    return SearchHelper.findNextWordEnd(
      (editor as IjVimEditor).editor,
      (caret as IjVimCaret).caret,
      count,
      bigWord,
    )
  }

  override fun findNextWordEnd(
    chars: CharSequence,
    pos: Int,
    size: Int,
    count: Int,
    bigWord: Boolean,
    spaceWords: Boolean,
  ): Int {
    return SearchHelper.findNextWordEnd(chars, pos, size, count, bigWord, spaceWords)
  }

  override fun findPattern(
    editor: VimEditor,
    pattern: String?,
    startOffset: Int,
    count: Int,
    searchOptions: EnumSet<SearchOptions>?,
  ): TextRange? {
    return SearchHelper.findPattern(editor.ij, pattern, startOffset, count, searchOptions)
  }

  override fun findNextCharacterOnLine(editor: VimEditor, caret: VimCaret, count: Int, ch: Char): Int {
    return SearchHelper.findNextCharacterOnLine(editor.ij, caret.ij, count, ch)
  }

  override fun findWordUnderCursor(
    editor: VimEditor,
    caret: VimCaret,
    count: Int,
    dir: Int,
    isOuter: Boolean,
    isBig: Boolean,
    hasSelection: Boolean,
  ): TextRange {
    return SearchHelper.findWordUnderCursor(editor.ij, caret.ij, count, dir, isOuter, isBig, hasSelection)
  }

  override fun findSentenceRange(editor: VimEditor, caret: VimCaret, count: Int, isOuter: Boolean): TextRange {
    return SearchHelper.findSentenceRange(editor.ij, caret.ij, count, isOuter)
  }

  override fun findParagraphRange(editor: VimEditor, caret: VimCaret, count: Int, isOuter: Boolean): TextRange? {
    return SearchHelper.findParagraphRange(editor.ij, caret.ij, count, isOuter)
  }

  override fun findBlockTagRange(editor: VimEditor, caret: VimCaret, count: Int, isOuter: Boolean): TextRange? {
    return SearchHelper.findBlockTagRange(editor.ij, caret.ij, count, isOuter)
  }

  override fun findBlockQuoteInLineRange(
    editor: VimEditor,
    caret: VimCaret,
    quote: Char,
    isOuter: Boolean,
  ): TextRange? {
    return SearchHelper.findBlockQuoteInLineRange(editor.ij, caret.ij, quote, isOuter)
  }

  override fun findBlockRange(
    editor: VimEditor,
    caret: VimCaret,
    type: Char,
    count: Int,
    isOuter: Boolean,
  ): TextRange? {
    return SearchHelper.findBlockRange(editor.ij, caret.ij, type, count, isOuter)
  }
}
