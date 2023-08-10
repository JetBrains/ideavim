/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection

/**
 * Matcher used to match against a character collection.
 *
 * @param chars       The individual characters in the collection
 * @param ranges      The ranges of characters in the collection
 * @param isNegated   Whether the Matcher should accept or refuse characters that are in the collection
 * @param includesEOL Whether the collection includes the end-of-line
 */
internal class CollectionMatcher(
  private val chars: List<Char> = emptyList(),
  private val ranges: List<CollectionRange> = emptyList(),
  private val isNegated: Boolean = false,
  private val includesEOL: Boolean = false
) : Matcher {
  override fun matches(editor: VimEditor, index: Int, groups: VimMatchGroupCollection): MatcherResult {
    if (index >= editor.text().length) return MatcherResult.Failure

    if (!includesEOL && editor.text()[index] == '\n') return MatcherResult.Failure
    if (includesEOL && editor.text()[index] == '\n') return MatcherResult.Success(1)

    val char = editor.text()[index]
    val result = (chars.contains(char) || ranges.any { it.inRange(char) }) == !isNegated
    return if (result) MatcherResult.Success(1)
    else MatcherResult.Failure
  }
}

/**
 * Represents a range of characters in a collection
 *
 * @param start The starting character of the range (inclusive)
 * @param end   The ending character of the range (inclusive)
 */
internal data class CollectionRange(val start: Char, val end: Char) {

  /**
   * Determines whether a character is inside the range
   *
   * @param char The character to verify
   *
   * @return whether char is inside the range
   */
  internal fun inRange(char: Char) : Boolean {
    return char.code in start.code..end.code
  }
}