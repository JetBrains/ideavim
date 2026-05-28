/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.common.TextRange

/**
 * Find the lhs candidate immediately to the left of [caretOffset], per Vim's `:help abbreviations`:
 *
 *   * If the char before the cursor is a keyword char, the lhs covers full-id and end-id: the
 *     trailing keyword char plus every preceding non-whitespace char that matches the class of
 *     the char two-before-cursor.
 *   * If the char before the cursor is a non-keyword char, the lhs covers non-id: every
 *     contiguous non-whitespace char up to and including it.
 *
 * Returns null when there is no preceding char on the current line.
 */
internal fun findAbbreviationLhsRange(text: CharSequence, caretOffset: Int, lineStart: Int): TextRange? {
  if (caretOffset <= lineStart) return null
  val start = if (isAbbreviationKeywordChar(text[caretOffset - 1])) {
    walkBackKeywordLhs(text, caretOffset, lineStart)
  } else {
    walkBackNonIdLhs(text, caretOffset, lineStart)
  }
  return TextRange(start, caretOffset)
}

internal fun isAbbreviationKeywordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

/**
 * Walk back from a keyword-ending cursor. The lhs is the trailing keyword char plus every
 * preceding non-whitespace char of the same class as the char two-before-cursor. If there is
 * no char two-before-cursor, the class defaults to keyword (matching Vim's full-id behavior).
 */
private fun walkBackKeywordLhs(text: CharSequence, caretOffset: Int, lineStart: Int): Int {
  val hasSecondChar = caretOffset - 1 > lineStart
  val expectedKeywordClass = if (hasSecondChar) isAbbreviationKeywordChar(text[caretOffset - 2]) else true
  var start = caretOffset - 1
  while (start > lineStart) {
    val c = text[start - 1]
    if (c.isWhitespace() || isAbbreviationKeywordChar(c) != expectedKeywordClass) break
    start--
  }
  return start
}

/** Walk back from a non-keyword-ending cursor through every contiguous non-whitespace char. */
private fun walkBackNonIdLhs(text: CharSequence, caretOffset: Int, lineStart: Int): Int {
  var start = caretOffset - 1
  while (start > lineStart && !text[start - 1].isWhitespace()) {
    start--
  }
  return start
}
