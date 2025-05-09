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

interface VimSearchHelper {
  /**
   * Find next paragraph bound offset
   * @param editor target editor
   * @param caret caret whose position will be used to start the search from
   * @param count search for the count-th occurrence
   * @param allowBlanks true if we consider lines with whitespaces as empty
   * @return next paragraph off
   */
  fun findNextParagraph(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    allowBlanks: Boolean,
  ): Int?

  fun findNextSentenceStart(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): Int?

  fun findSection(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    type: Char,
    direction: Int,
    count: Int,
  ): Int

  /**
   * @param chars         the char sequence to search in
   * @param startIndex    the start index (inclusive)
   * @param count         search for the count-th occurrence
   * @return offset of next camelCase or snake_case word start or null if nothing was found
   */
  fun findNextCamelStart(chars: CharSequence, startIndex: Int, count: Int): Int?

  /**
   * @param chars         the char sequence to search in
   * @param endIndex      the end index (exclusive)
   * @param count         search for the count-th occurrence
   * @return offset of count-th previous camelCase or snake_case word start or null if nothing was found
   */
  fun findPreviousCamelStart(chars: CharSequence, endIndex: Int, count: Int): Int?

  /**
   * @param chars         the char sequence to search in
   * @param startIndex    the start index (inclusive)
   * @param count         search for the count-th occurrence
   * @return offset of next camelCase or snake_case word start or null if nothing was found
   */
  fun findNextCamelEnd(chars: CharSequence, startIndex: Int, count: Int): Int?

  /**
   * @param chars         the char sequence to search in
   * @param endIndex      the end index (exclusive)
   * @param count         search for the count-th occurrence
   * @return offset of count-th previous camelCase or snake_case word end or null if nothing was found
   */
  fun findPreviousCamelEnd(chars: CharSequence, endIndex: Int, count: Int): Int?

  fun findNextSentenceEnd(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    countCurrent: Boolean,
    requireAll: Boolean,
  ): Int?

  fun findMethodEnd(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
  ): Int

  fun findMethodStart(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
  ): Int

  /**
   * Find the next word in the editor's document, from the given starting point
   *
   * Note that this function can return an out of bounds index when there is no next word!
   *
   * @param editor The editor's document to search in. Editor is required because word boundaries depend on
   *               local-to-buffer options
   * @param searchFrom The offset in the document to search from
   * @param count      Return an offset to the [count] word from the starting position. Will search backwards if negative
   * @param bigWord    Use WORD instead of word boundaries
   * @return The offset of the [count] next word, or `0` or the offset of the end of file if not found
   */
  fun findNextWord(editor: VimEditor, searchFrom: Int, count: Int, bigWord: Boolean): Int

  /**
   * Find the next word in some text outside the editor (e.g., command line), from the given starting point
   *
   * Note that this function can return an out of bounds index when there is no next word!
   *
   * @param text        The text to search in
   * @param editor Required because word boundaries depend on local-to-buffer options
   * @param searchFrom The offset in the document to search from
   * @param count      Return an offset to the [count] word from the starting position. Will search backwards if negative
   * @param bigWord    Use WORD instead of word boundaries
   * @return The offset of the [count] next word, or `0` or the offset of the end of file if not found
   */
  fun findNextWord(
    text: CharSequence,
    editor: VimEditor,
    searchFrom: Int,
    count: Int,
    bigWord: Boolean,
  ): Int

  /**
   * Find the end offset of the next word in the editor's document, from the given starting point
   *
   * @param editor The editor's document to search in. Editor is required because word boundaries depend on
   *               local-to-buffer options
   * @param searchFrom The offset in the document to search from
   * @param count      Return an offset to the [count] word from the starting position. Will search backwards if negative
   * @param bigWord    Use WORD instead of word boundaries
   * @param stopOnEmptyLine Vim considers an empty line to be a word/WORD, but `e` and `E` don't respect this for vi
   *                        compatibility reasons. Callers other than `e` and `E` should pass `true`
   * @return The offset of the [count] next word/WORD. Will return document bounds if not found
   */
  fun findNextWordEnd(
    editor: VimEditor,
    searchFrom: Int,
    count: Int,
    bigWord: Boolean,
    stopOnEmptyLine: Boolean = true,
  ): Int

  /**
   * Find text matching the given pattern.
   *
   * @see :help /pattern
   *
   * @param editor          The editor to search in
   * @param pattern         The pattern to search for
   * @param startOffset     The offset to start searching from
   * @param count           Find the nth next occurrence of the pattern. Must be 1 or greater.
   * @param searchOptions   A set of options, such as direction and wrap
   * @return                A TextRange representing the result, or null
   */
  fun findPattern(
    editor: VimEditor,
    pattern: String?,
    startOffset: Int,
    count: Int,
    searchOptions: EnumSet<SearchOptions>?,
  ): TextRange?

  /**
   * Find all occurrences of the pattern.
   *
   * @param editor      The editor to search in
   * @param pattern     The pattern to search for
   * @param startLine   The start line of the range to search for
   * @param endLine     The end line of the range to search for, or -1 for the whole document
   * @param ignoreCase  Case sensitive or insensitive searching
   * @return            A list of TextRange objects representing the results
   */
  fun findAll(
    editor: VimEditor,
    pattern: String,
    startLine: Int,
    endLine: Int,
    ignoreCase: Boolean,
  ): List<TextRange>

  fun findNextCharacterOnLine(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    ch: Char,
  ): Int

  /**
   * Find the word at or nearest to the current caret offset
   *
   * Note that this is not a word text object!
   *
   * This function is used to get the word to search for using the `*`/`#` and `g*`/`g#` operators. It will return:
   * * the range of the keyword under the cursor
   * * or the first keyword after the cursor on the current line,
   * * or the non-blank word under the cursor,
   * * or the first non-blank word after the cursor on the current line
   * * or null, if none of the above are found
   */
  fun findWordNearestCursor(editor: VimEditor, caret: ImmutableVimCaret): TextRange?

  /**
   * Find the range of the word text object at the location of the caret
   */
  fun findWordObject(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    isOuter: Boolean,
    isBig: Boolean,
  ): TextRange

  fun findSentenceRange(
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
  fun findParagraphRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    isOuter: Boolean,
  ): TextRange?

  fun findBlockTagRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
    isOuter: Boolean,
  ): TextRange?

  fun findBlockQuoteInLineRange(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    quote: Char,
    isOuter: Boolean,
  ): TextRange?

  fun findMisspelledWord(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    count: Int,
  ): Int
}
