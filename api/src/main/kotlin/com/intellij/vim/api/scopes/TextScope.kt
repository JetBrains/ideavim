/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

/**
 * Scope that provides text pattern matching and word-boundary utilities.
 *
 * Example usage:
 * ```kotlin
 * // Lambda style
 * val found = api.text { matches("\\w+", "hello") }
 *
 * // Direct object style
 * val offset = api.text().getNextCamelStartOffset(chars, 0)
 * ```
 */
@VimApiDsl
interface TextScope {
  /**
   * Checks if a pattern matches a text.
   *
   * @param pattern The regular expression pattern to match
   * @param text The text to check against the pattern
   * @param ignoreCase Whether to ignore case when matching
   * @return True if the pattern matches the text, false otherwise
   */
  suspend fun matches(pattern: String, text: String, ignoreCase: Boolean = false): Boolean

  /**
   * Finds all matches of a pattern in a text.
   *
   * @param text The text to search in
   * @param pattern The regular expression pattern to search for
   * @return A list of pairs representing the start and end offsets of each match
   */
  suspend fun getAllMatches(text: String, pattern: String): List<Pair<Int, Int>>

  /**
   * Finds the start offset of the next word in camel case or snake case text.
   *
   * @param chars The character sequence to search in (e.g., document text)
   * @param startIndex The index to start searching from (inclusive). Must be within the bounds of chars: [0, chars.length)
   * @param count Find the [count]-th occurrence. Must be greater than 0.
   * @return The offset of the next word start, or null if not found
   */
  suspend fun getNextCamelStartOffset(chars: CharSequence, startIndex: Int, count: Int = 1): Int?

  /**
   * Finds the start offset of the previous word in camel case or snake case text.
   *
   * @param chars The character sequence to search in (e.g., document text)
   * @param endIndex The index to start searching backward from (exclusive). Must be within the bounds of chars: [0, chars.length]
   * @param count Find the [count]-th occurrence. Must be greater than 0.
   * @return The offset of the previous word start, or null if not found
   */
  suspend fun getPreviousCamelStartOffset(chars: CharSequence, endIndex: Int, count: Int = 1): Int?

  /**
   * Finds the end offset of the next word in camel case or snake case text.
   *
   * @param chars The character sequence to search in (e.g., document text)
   * @param startIndex The index to start searching from (inclusive). Must be within the bounds of chars: [0, chars.length)
   * @param count Find the [count]-th occurrence. Must be greater than 0.
   * @return The offset of the next word end, or null if not found
   */
  suspend fun getNextCamelEndOffset(chars: CharSequence, startIndex: Int, count: Int = 1): Int?

  /**
   * Finds the end offset of the previous word in camel case or snake case text.
   *
   * @param chars The character sequence to search in (e.g., document text)
   * @param endIndex The index to start searching backward from (exclusive). Must be within the bounds of chars: [0, chars.length]
   * @param count Find the [count]-th occurrence. Must be greater than 0.
   * @return The offset of the previous word end, or null if not found
   */
  suspend fun getPreviousCamelEndOffset(chars: CharSequence, endIndex: Int, count: Int = 1): Int?
}
