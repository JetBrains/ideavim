/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

/**
 * Searches for a single character and returns its index. Supports optional escaping.
 *
 * @param char The character to look for.
 * @param startIndex The index to start the search from (inclusive).
 * @param endIndex The index to end the search (exclusive).
 * @param escaped If true, the method returns the index of char only if it's escaped. If false, it returns the index only if it's not escaped. If null, escaping is not considered.
 * @return The index of the character, or null if it could not be located within the specified range.
 */
fun CharSequence.indexOfOrNull(
  char: Char,
  startIndex: Int = 0,
  endIndex: Int = length,
  escaped: Boolean? = null,
): Int? {
  return indexOfAnyOrNull(charArrayOf(char), startIndex, endIndex, escaped)
}

/**
 * Searches for any character from the specified array and returns its index. Supports optional escaping.
 *
 * @param chars An array of characters to search for.
 * @param startIndex The index to start the search from (inclusive).
 * @param endIndex The index to end the search (exclusive).
 * @param escaped If true, the method returns the index of char only if it's escaped. If false, it returns the index only if it's not escaped. If null, escaping is not considered.
 * @return The first index of any character from the array, or null if none could be found within the specified range.
 */
fun CharSequence.indexOfAnyOrNull(
  chars: CharArray,
  startIndex: Int = 0,
  endIndex: Int = length,
  escaped: Boolean? = null,
): Int? {
  for (i in startIndex until kotlin.math.min(endIndex, length)) {
    if (chars.contains(get(i)) &&
      (
        escaped == null ||
          (escaped == true && isEscaped(this, i)) ||
          (escaped == false && !isEscaped(this, i))
        )
    ) {
      return i
    }
  }
  return null
}

/**
 * Searches for any character from the specified array in reverse and returns its index. Supports optional escaping.
 *
 * @param chars An array of characters to search for.
 * @param startIndex The index to start the search from (in reverse). The default is the end of the string.
 * @param endIndex The index to finish the search, exclusive. If the index is -1, the last checked index will be 0. By default, it's the start of the string.
 * @param escaped If true, the method returns the index of char only if it's escaped. If false, it returns the index only if it's not escaped. If null, escaping is not considered.
 * @return The last index of any character from the array, or null if none could be found within the specified range.
 */
fun CharSequence.lastIndexOfAnyOrNull(
  chars: CharArray,
  startIndex: Int = lastIndex,
  endIndex: Int = -1,
  escaped: Boolean? = null,
): Int? {
  for (i in startIndex downTo kotlin.math.max(endIndex + 1, 0)) {
    if (chars.contains(get(i)) &&
      (
        escaped == null ||
          (escaped == true && isEscaped(this, i)) ||
          (escaped == false && !isEscaped(this, i))
        )
    ) {
      return i
    }
  }
  return null
}

/**
 * Checks whether a character at the specified position is escaped (preceded by an odd number of backslashes).
 *
 * @param chars The character sequence.
 * @param pos The position of the character to check.
 * @return true if the character is escaped, and false otherwise.
 */
private fun isEscaped(chars: CharSequence, pos: Int): Boolean {
  var backslashCounter = 0

  var i = pos
  while (i-- > 0 && chars[i] == '\\') {
    backslashCounter++
  }
  return backslashCounter % 2 != 0
}
