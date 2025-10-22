/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.engine.nfa.matcher

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection

/**
 * Matcher used to match against a character collection.
 *
 * @param chars             The individual characters in the collection
 * @param ranges            The ranges of characters in the collection
 * @param isNegated         Whether the Matcher should accept or refuse characters that are in the collection
 * @param includesEOL       Whether the collection includes the end-of-line
 * @param forceNoIgnoreCase If this is set, matching is always case-sensitive
 */
internal class CollectionMatcher(
  private val chars: Set<Char> = emptySet(),
  private val ranges: List<CollectionRange> = emptyList(),
  private val charClasses: List<(Char) -> Boolean> = emptyList(),
  private val isNegated: Boolean = false,
  private val includesEOL: Boolean = false,
  private val forceNoIgnoreCase: Boolean = false,
) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    if (index >= editor.text().length) return MatcherResult.Failure

    val currentChar = editor.text()[index]

    // Handle end-of-line character
    if (currentChar == '\n') {
      return if (includesEOL) MatcherResult.Success(1) else MatcherResult.Failure
    }

    // Check if character matches the collection
    val matchesCollection = if (isCaseInsensitive && !forceNoIgnoreCase) {
      matchesCharCaseInsensitive(currentChar)
    } else {
      matchesCharCaseSensitive(currentChar)
    }

    // Apply negation if needed
    val result = matchesCollection == !isNegated
    return if (result) MatcherResult.Success(1) else MatcherResult.Failure
  }

  private fun matchesCharCaseSensitive(char: Char): Boolean {
    return chars.contains(char) ||
           ranges.any { it.inRange(char) } ||
           charClasses.any { it(char) }
  }

  private fun matchesCharCaseInsensitive(char: Char): Boolean {
    val lowerChar = char.lowercaseChar()
    val upperChar = char.uppercaseChar()

    return chars.any { it.equals(lowerChar, ignoreCase = true) } ||
           ranges.any { it.inRange(char, isCaseInsensitive = true) } ||
           charClasses.any { it(lowerChar) || it(upperChar) }
  }

  override fun isEpsilon(): Boolean {
    return false
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
   * @param char              The character to verify
   * @param isCaseInsensitive Whether case should be ignored
   *
   * @return whether char is inside the range
   */
  internal fun inRange(char: Char, isCaseInsensitive: Boolean = false): Boolean {
    return if (isCaseInsensitive) char.lowercaseChar().code in start.lowercaseChar().code..end.lowercaseChar().code
    else char.code in start.code..end.code
  }
}