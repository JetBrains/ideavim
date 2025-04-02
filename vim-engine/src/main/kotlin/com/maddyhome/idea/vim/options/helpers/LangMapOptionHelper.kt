/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options.helpers

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.Graphemes
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

// `'langmap'` maps a typed char to an ASCII char
object LangMapOptionHelper {
  fun mapChar(from: Char) =
    injector.optionGroup.getParsedEffectiveOptionValue(Options.langmap, null, ::parse).getOrDefault(from, from)

  fun split(langMap: String): List<String> {
    // Negative lookbehind regex to make sure we don't split on escaped commas
    return langMap.split("""(?<!\\),""".toRegex())
  }

  private fun parse(langMap: VimString) = parse(langMap.value, "langmap=$langMap")

  /**
   * Parses the `'langmap'` option value into a map of Unicode char to Vim's ASCII char commands
   *
   * The option value is a set of comma-separated parts. Each part can be a single pair, multiple pairs or a sequence of
   * "from" characters, a semicolon and a sequence of "to" characters.
   *
   * The "from" character is in the user's preferred language, while the "to" character is (usually) a Vim command char,
   * which are ASCII. These characters are represented with [Char], so are only 16-bit. For the purposes of `'langmap'`,
   * it is not expected that the user can type a 32-bit codepoint with a single key press. Similarly, the "to" char is
   * not expected to be a 32-bit codepoint that would be further mapped to VIM's ASCII command set. If these assumptions
   * prove untrue, we'll need to switch to [Int] codepoints and [Graphemes].
   */
  fun parse(langMap: String, token: String): Map<Char, Char> {
    if (langMap.isEmpty()) return emptyMap()

    return buildMap {
      // Match a semicolon preceded by an escaped backslash (`\\;`) or a semicolon not preceded by a single backslash
      val semicolonRegex = """(?<=\\\\);|(?<!\\);""".toRegex()

      // Each comma-separated part is either a sequence of character pair or a sequence of from chars and a sequence of
      // to chars separated with a semicolon. Note that semicolon might be used as a from/to value and can be
      // delimited
      split(langMap).forEach { part ->
        if (part.isEmpty()) return@forEach

        // We don't want to split on a regex here, because that would split a string like `abc;A;C` into 3 parts, rather
        // than 2 parts with a valid (albeit unescaped) semicolon in the second part. However, make sure we don't split
        // on an escaped semicolon.
        val match = semicolonRegex.find(part)
        if (match != null) {
          processSemicolonSeparatedPairs(part, match, token)
        }
        else {
          processCharacterPairs(part, token)
        }
      }
    }
  }

  private fun MutableMap<Char, Char>.processSemicolonSeparatedPairs(
    part: String,
    match: MatchResult,
    token: String,
  ) {
    val from = part.substring(0, match.range.first)
    val to = part.substring(match.range.last + 1)

    // Special case for empty from string: `langmap=;ABC`
    if (from.isEmpty() && to.isNotEmpty()) {
      processCharacterPairs(part.substring(1), token)
      return
    }

    var fromIndex = 0
    var toIndex = 0
    while (fromIndex < from.length) {
      var fromChar = from[fromIndex]
      if (fromChar == '\\') {
        fromIndex++
        fromChar = from[fromIndex]  // This can't fail - we can't have a trailing backslash before the semicolon
      }

      if (toIndex >= to.length) {
        throw exExceptionMessage("E357", fromChar, token) // E357: 'langmap': Matching character missing for {0}: {1}
      }

      var toChar = to[toIndex]
      if (toChar == '\\') {
        toIndex++
        if (toIndex >= to.length) {
          throw exExceptionMessage("E357", fromChar, token) // E357: 'langmap': Matching character missing for {0}: {1}
        }
        toChar = to[toIndex]
      }

      put(fromChar, toChar)

      fromIndex++
      toIndex++

      if (fromIndex >= from.length && toIndex < to.length) {
        throw exExceptionMessage("E358", to.substring(toIndex), token) // E358: 'langmap': Extra characters after semicolon: {0}: {1}
      }
    }
  }

  private fun MutableMap<Char, Char>.processCharacterPairs(
    part: String,
    token: String,
  ) {
    var index: Int? = 0
    while (index != null && index < part.length) {
      var fromChar = part[index]
      if (fromChar == '\\') {
        index = getNextMatchingCharFromPair(part, index, fromChar, token)
        fromChar = part[index]
      }
      index = getNextMatchingCharFromPair(part, index, fromChar, token)

      var toChar = part[index]
      if (toChar == '\\') {
        index = getNextMatchingCharFromPair(part, index, fromChar, token)
        toChar = part[index]
      }

      put(fromChar, toChar)

      index++
    }
  }

  private fun getNextMatchingCharFromPair(string: String, index: Int, lastChar: Char, token: String): Int {
    val next = index + 1
    if (next >= string.length) {
      throw exExceptionMessage("E357", lastChar, token)  // E357: 'langmap': Matching character missing for {0}: {1}
    }
    return next
  }
}
