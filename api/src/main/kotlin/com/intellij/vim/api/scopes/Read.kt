/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.CaretData
import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Jump
import com.intellij.vim.api.Line
import com.intellij.vim.api.Mark
import com.intellij.vim.api.Range
import com.intellij.vim.api.scopes.caret.CaretRead

@VimPluginDsl
interface Read {
  val textLength: Long
  val text: CharSequence
  val lineCount: Int

  suspend fun <T> forEachCaret(block: suspend CaretRead.() -> T): List<T>
  suspend fun with(caretId: CaretId, block: suspend CaretRead.() -> Unit)
  suspend fun withPrimaryCaret(block: suspend CaretRead.() -> Unit)

  suspend fun getLineStartOffset(line: Int): Int
  suspend fun getLineEndOffset(line: Int, allowEnd: Boolean): Int

  suspend fun getLine(offset: Int): Line

  val caretData: List<CaretData>
  val caretIds: List<CaretId>

  /**
   * Gets a global mark by its character key.
   *
   * @param char The character key of the mark (A-Z)
   * @return The mark, or null if the mark doesn't exist
   */
  suspend fun getGlobalMark(char: Char): Mark?

  /**
   * All global marks.
   */
  val globalMarks: Set<Mark>

  /**
   * Gets a jump from the jump list.
   *
   * @param count The number of jumps to go back (negative) or forward (positive) from the current position in the jump list.
   * @return The jump, or null if there is no jump at the specified position
   */
  suspend fun getJump(count: Int = 0): Jump?

  /**
   * Gets all jumps in the jump list.
   *
   * @return A list of all jumps
   */
  val jumps: List<Jump>

  /**
   * Index of the current position in the jump list.
   *
   * This is used to determine which jump will be used when navigating with Ctrl-O and Ctrl-I.
   */
  val currentJumpIndex: Int

  /**
   * Scrolls the caret into view.
   *
   * This ensures that the caret is visible in the editor window.
   */
  suspend fun scrollCaretIntoView()

  /**
   * Scrolls the editor by a specified number of lines.
   *
   * @param lines The number of lines to scroll. Positive values scroll down, negative values scroll up.
   * @return True if the scroll was successful, false otherwise
   */
  suspend fun scrollVertically(lines: Int): Boolean

  /**
   * Scrolls the current line to the top of the display.
   *
   * @param line The line number to scroll to (1-based). If 0, uses the current line.
   * @param start Whether to position the caret at the start of the line
   * @return True if the scroll was successful, false otherwise
   */
  suspend fun scrollLineToTop(line: Int, start: Boolean): Boolean

  /**
   * Scrolls the current line to the middle of the display.
   *
   * @param line The line number to scroll to (1-based). If 0, uses the current line.
   * @param start Whether to position the caret at the start of the line
   * @return True if the scroll was successful, false otherwise
   */
  suspend fun scrollLineToMiddle(line: Int, start: Boolean): Boolean

  /**
   * Scrolls the current line to the bottom of the display.
   *
   * @param line The line number to scroll to (1-based). If 0, uses the current line.
   * @param start Whether to position the caret at the start of the line
   * @return True if the scroll was successful, false otherwise
   */
  suspend fun scrollLineToBottom(line: Int, start: Boolean): Boolean

  /**
   * Scrolls the editor horizontally by a specified number of columns.
   *
   * @param columns The number of columns to scroll. Positive values scroll right, negative values scroll left.
   * @return True if the scroll was successful, false otherwise
   */
  suspend fun scrollHorizontally(columns: Int): Boolean

  /**
   * Scrolls the editor to position the caret column at the left edge of the display.
   *
   * @return True if the scroll was successful, false otherwise
   */
  suspend fun scrollCaretToLeftEdge(): Boolean

  /**
   * Scrolls the editor to position the caret column at the right edge of the display.
   *
   * @return True if the scroll was successful, false otherwise
   */
  suspend fun scrollCaretToRightEdge(): Boolean

  /**
   * Find the next paragraph-bound offset in the editor.
   *
   * @param startLine Line to start the search from.
   * @param count Search for the [count]-th occurrence.
   * @param includeWhitespaceLines Should be `true` if we consider lines with whitespaces as empty.
   * @return next paragraph off
   */
  suspend fun getNextParagraphBoundOffset(startLine: Int, count: Int = 1, includeWhitespaceLines: Boolean = true): Int?

  /**
   * Finds the next sentence start in the editor from the given offset, based on the specified parameters.
   *
   * @param count Search for the [count]-th occurrence.
   * @param includeCurrent If `true`, includes the current sentence if at its boundary.
   * @param requireAll If `true`, returns `null` if fewer than [count] sentences are found.
   * @return The offset of the next sentence start, or `null` if not found or constraints cannot be met.
   */
  suspend fun getNextSentenceStart(startOffset: Int, count: Int = 1, includeCurrent: Boolean, requireAll: Boolean = true): Int?

  /**
   * Find the next section in the editor.
   *
   * @param startLine The line to start searching from.
   * @param marker The type of section to find.
   * @param count Search for the [count]-th occurrence.
   * @return The offset of the next section.
   */
  suspend fun getNextSectionStart(startLine: Int, marker: Char, count: Int = 1): Int

  /**
   * Find the start of the previous section in the editor.
   *
   * @param startLine The line to start searching from.
   * @param marker The type of section to find.
   * @param count Search for the [count]-th occurrence.
   * @return The offset of the next section.
   */
  suspend fun getPreviousSectionStart(startLine: Int, marker: Char, count: Int = 1): Int

  /**
   * Find the next sentence end from the given offset.
   *
   * @param startOffset The offset to start searching from
   * @param count Search for the [count]-th occurrence.
   * @param includeCurrent Whether to count the current position as a sentence end
   * @param requireAll Whether to require all sentence ends to be found
   * @return The offset of the next sentence end, or null if not found
   */
  suspend fun getNextSentenceEnd(startOffset: Int, count: Int = 1, includeCurrent: Boolean, requireAll: Boolean = true): Int?

  /**
   * Find the next word in the editor's document, from the given starting point
   *
   * @param startOffset The offset in the document to search from
   * @param count Search for the [count]-th occurrence. If negative, search backwards.
   * @param isBigWord Use WORD instead of word boundaries.
   * @return The offset of the [count]-th next word, or `null` if not found.
   */
  suspend fun getNextWordStartOffset(startOffset: Int, count: Int = 1, isBigWord: Boolean): Int?

  /**
   * Find the end offset of the next word in the editor's document, from the given starting point
   *
   * @param startOffset The offset in the document to search from
   * @param count Return an offset to the [count] word from the starting position. Will search backwards if negative
   * @param isBigWord Use WORD instead of word boundaries
   * @param stopOnEmptyLine Vim considers an empty line to be a word/WORD, but `e` and `E` don't respect this for vi
   *                        compatibility reasons. Callers other than `e` and `E` should pass `true`
   * @return The offset of the [count] next word/WORD. Will return document bounds if not found
   */
  suspend fun getNextWordEndOffset(startOffset: Int, count: Int = 1, isBigWord: Boolean, stopOnEmptyLine: Boolean = true): Int

  /**
   * Find the next character on the current line
   *
   * @param startOffset The offset to start searching from
   * @param count The number of occurrences to find
   * @param char The character to find
   * @return The offset of the next character, or -1 if not found
   */
  suspend fun getNextCharOnLineOffset(startOffset: Int, count: Int = 1, char: Char): Int

  /**
   * Find the word at or nearest to the given offset
   *
   * @param startOffset The offset to search from
   * @return The range of the word, or null if not found
   */
  suspend fun getNearestWordOffset(startOffset: Int): Range?

  /**
   * Returns range of a paragraph containing the given line.
   *
   * @param line line to start the search from
   * @param count search for the count paragraphs forward
   * @param isOuter true if it is an outer motion, false otherwise
   * @return the paragraph text range
   */
  suspend fun getParagraphRange(line: Int, count: Int = 1, isOuter: Boolean): Range?

  /**
   * Find a block quote in the current line
   *
   * @param startOffset The offset to start searching from
   * @param quote The quote character to find
   * @param isOuter Whether to include the quotes in the range
   * @return The range of the block quote, or null if not found
   */
  suspend fun getBlockQuoteInLineRange(startOffset: Int, quote: Char, isOuter: Boolean): Range?

  /**
   * Finds all occurrences of the given pattern within a specified line range.
   *
   * This suspend function searches for all matches of a pattern within a specified range of lines
   * in the document. It's useful for implementing commands like `:g/pattern/` or `:v/pattern/`
   * that need to find all occurrences of a pattern.
   *
   * @param pattern The pattern to search for. This is a plain string, not a regex pattern.
   * @param startLine The line number to start searching from (0-based). Must be within the range [0, lineCount-1].
   * @param endLine The line number to end searching at (0-based), or -1 for the whole document.
   *               If specified, must be within the range [startLine, lineCount-1].
   * @param ignoreCase If true, performs case-insensitive search; if false, performs case-sensitive search.
   * @return A list of Ranges representing all matches found. Empty list if no matches are found.
   */
  suspend fun findAll(pattern: String, startLine: Int, endLine: Int, ignoreCase: Boolean = false): List<Range>

  /**
   * Finds text matching the given Vim-style regular expression pattern.
   *
   * This suspend function implements Vim's pattern search functionality, supporting all Vim regex syntax.
   * See `:help /pattern` in Vim for details on the pattern syntax.
   *
   * @param pattern The Vim-style regex pattern to search for.
   * @param startOffset The offset to start searching from. Must be within the range [0, document.length].
   * @param count Find the [count]-th occurrence of the pattern.
   * @param backwards If true, search backward from the start offset; if false, search forward.
   * @return A Range representing the matched text, or null if no match is found.
   */
  suspend fun findPattern(pattern: String, startOffset: Int, count: Int = 1, backwards: Boolean = false): Range?
}
