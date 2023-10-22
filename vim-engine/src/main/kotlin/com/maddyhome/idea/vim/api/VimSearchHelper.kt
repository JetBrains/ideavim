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
  /**
   * Find next paragraph bound offset
   * @param editor target editor
   * @param caret caret whose position will be used to start the search from
   * @param count search for the count-th occurrence
   * @param allowBlanks true if we consider lines with whitespaces as empty
   * @return next paragraph off
   */
  public fun findNextParagraph(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    allowBlanks: Boolean,
  ): Int?

  public fun findNextSentenceStart(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): Int?

  public fun findSection(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    type: Char,
    dir: Int,
    count: Int,
  ): Int

  /**
   * @param chars         the char sequence to search in
   * @param startIndex    the start index (inclusive)
   * @param count         search for the count-th occurrence
   * @return offset of next camelCase or snake_case word start or null if nothing was found
   */
  public fun findNextCamelStart(chars: CharSequence, startIndex: Int, count: Int): Int?

  /**
   * @param chars         the char sequence to search in
   * @param endIndex      the end index (exclusive)
   * @param count         search for the count-th occurrence
   * @return offset of count-th previous camelCase or snake_case word start or null if nothing was found
   */
  public fun findPreviousCamelStart(chars: CharSequence, endIndex: Int, count: Int): Int?

  /**
   * @param chars         the char sequence to search in
   * @param startIndex    the start index (inclusive)
   * @param count         search for the count-th occurrence
   * @return offset of next camelCase or snake_case word start or null if nothing was found
   */
  public fun findNextCamelEnd(chars: CharSequence, startIndex: Int, count: Int): Int?

  /**
   * @param chars         the char sequence to search in
   * @param endIndex      the end index (exclusive)
   * @param count         search for the count-th occurrence
   * @return offset of count-th previous camelCase or snake_case word end or null if nothing was found
   */
  public fun findPreviousCamelEnd(chars: CharSequence, endIndex: Int, count: Int): Int?

  public fun findNextSentenceEnd(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): Int?

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

  /**
   * Find the next word in the editor's document, from the given starting point
   *
   * @param editor The editor's document to search in. Editor is required because word boundaries depend on
   *               local-to-buffer options
   * @param searchFrom The offset in the document to search from
   * @param count      Return an offset to the [count] word from the starting position. Will search backwards if negative
   * @param bigWord    Use WORD instead of word boundaries
   * @param spaceWords Include whitespace as part of a word, e.g. the difference between `iw` and `aw` motions
   * @return The offset of the [count] next word, or `0` or the offset of the end of file if not found
   */
  public fun findNextWord(editor: VimEditor, searchFrom: Int, count: Int, bigWord: Boolean, spaceWords: Boolean): Int

  /**
   * Find the end offset of the next word in the editor's document, from the given starting point
   *
   * @param editor The editor's document to search in. Editor is required because word boundaries depend on
   *               local-to-buffer options
   * @param searchFrom The offset in the document to search from
   * @param count      Return an offset to the [count] word from the starting position. Will search backwards if negative
   * @param bigWord    Use WORD instead of word boundaries
   * @param spaceWords Include whitespace as part of a word, e.g. the difference between `iw` and `aw` motions
   * @return The offset of the [count] next word, or `0` or the offset of the end of file if not found
   */
  public fun findNextWordEnd(editor: VimEditor, searchFrom: Int, count: Int, bigWord: Boolean, spaceWords: Boolean): Int

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

  /**
   * Returns range of a paragraph containing the given caret
   * @param editor target editor
   * @param caret caret whose position will be used to start the search from
   * @param count search for the count paragraphs forward
   * @param isOuter true if it is an outer motion, false otherwise
   * @return the paragraph text range
   */
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
