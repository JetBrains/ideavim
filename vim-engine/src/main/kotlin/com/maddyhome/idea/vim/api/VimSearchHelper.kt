/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.SearchOptions
import java.util.*

public interface VimSearchHelper {
  public fun findNextParagraph(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    allowBlanks: Boolean,
  ): Int

  public fun findNextSentenceStart(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): Int

  public fun findSection(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    type: Char,
    dir: Int,
    count: Int,
  ): Int

  public fun findNextCamelEnd(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
  ): Int

  public fun findNextSentenceEnd(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): Int

  public fun findNextCamelStart(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
  ): Int

  public fun findMethodEnd(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
  ): Int

  public fun findMethodStart(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
  ): Int

  public fun findUnmatchedBlock(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    type: Char,
    count: Int,
  ): Int

  public fun findNextWordEnd(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    bigWord: Boolean,
  ): Int

  public fun findNextWordEnd(
    chars: CharSequence,
    pos: Int,
    size: Int,
    count: Int,
    bigWord: Boolean,
    spaceWords: Boolean,
  ): Int

  public fun findNextWord(editor: VimEditor, searchFrom: Int, count: Int, bigWord: Boolean): Long

  public fun findPattern(
    editor: VimEditor,
    pattern: String?,
    startOffset: Int,
    count: Int,
    searchOptions: EnumSet<SearchOptions>?,
  ): TextRange?

  public fun findNextCharacterOnLine(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    ch: Char,
  ): Int

  public fun findWordUnderCursor(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    dir: Int,
    isOuter: Boolean,
    isBig: Boolean,
    hasSelection: Boolean,
  ): TextRange

  public fun findSentenceRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    isOuter: Boolean,
  ): TextRange

  public fun findParagraphRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    isOuter: Boolean,
  ): TextRange?

  public fun findBlockTagRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    isOuter: Boolean,
  ): TextRange?

  public fun findBlockQuoteInLineRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    quote: Char,
    isOuter: Boolean,
  ): TextRange?

  public fun findBlockRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    type: Char,
    count: Int,
    isOuter: Boolean,
  ): TextRange?
}
