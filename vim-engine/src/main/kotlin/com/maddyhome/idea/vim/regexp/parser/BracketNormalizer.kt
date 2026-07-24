/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser

/**
 * Normalizes square brackets in a Vim pattern so the ANTLR grammar only ever sees
 * structurally valid collections.
 *
 * Vim assigns a literal meaning to a square bracket whenever it cannot be interpreted as
 * part of a collection. This preprocessor makes those literal brackets explicit by
 * escaping them, mirroring Vim's own `skip_anyof` logic:
 *
 * - An opening bracket with no matching closing bracket is a literal '['. For example,
 *   "heap[" matches the text "heap[", "[[[" matches "[[[", and "[a-z" matches "[a-z".
 * - A ']' that appears as the first member of a collection (right after '[' or '[^') is a
 *   literal member of that collection. For example, "[]]" is equivalent to "[\]]" and
 *   "[^]]" is equivalent to "[^\]]".
 *
 * The ANTLR lexer cannot express this on its own because it unconditionally treats '[' as
 * the start of a collection and the first ']' as its end. Running this pass first keeps the
 * grammar simple while matching Vim's behavior.
 */
internal object BracketNormalizer {

  private enum class Magic { MAGIC, VERY_MAGIC, NOMAGIC, VERY_NOMAGIC }

  /**
   * Characters that form a two-character escape sequence when preceded by a backslash
   * inside a collection. Mirrors Vim's REGEXP_INRANGE (`]^-n\`) plus REGEXP_ABBR
   * (`nrtebdoxuU`), which together are recognised inside `[...]` (see `skip_anyof` in
   * Vim/Neovim's regexp.c). The abbreviation letters never affect where the matching ']'
   * is found, but are listed for fidelity with Vim.
   */
  private const val IN_RANGE_ESCAPES = "]^-n\\rtebdoxuU"

  fun normalize(pattern: String): String {
    val sb = StringBuilder(pattern.length)
    var i = 0
    var magic = Magic.MAGIC
    while (i < pattern.length) {
      val c = pattern[i]
      if (c == '\\' && i + 1 < pattern.length) {
        val next = pattern[i + 1]
        when (next) {
          'v' -> { magic = Magic.VERY_MAGIC; sb.append("\\v"); i += 2 }
          'V' -> { magic = Magic.VERY_NOMAGIC; sb.append("\\V"); i += 2 }
          'm' -> { magic = Magic.MAGIC; sb.append("\\m"); i += 2 }
          'M' -> { magic = Magic.NOMAGIC; sb.append("\\M"); i += 2 }
          '[' -> i = if (magic == Magic.NOMAGIC || magic == Magic.VERY_NOMAGIC) {
            // In no-magic modes '\[' opens a collection.
            handleOpener(pattern, i, 2, sb)
          } else {
            // In magic modes '\[' is a literal '['.
            sb.append("\\["); i + 2
          }
          '%' -> {
            // "\%[" (optionally matched sequence), "\%(" (group), "\%#", ... The '[' in
            // "\%[" must not be treated as a collection, so copy "\%" plus the following
            // character verbatim.
            if (i + 2 < pattern.length) { sb.append(pattern, i, i + 3); i += 3 } else { sb.append("\\%"); i += 2 }
          }
          else -> { sb.append(c).append(next); i += 2 }
        }
        continue
      }
      // In very-magic mode "%[" is an optionally matched sequence, not a collection.
      if (c == '%' && magic == Magic.VERY_MAGIC && i + 1 < pattern.length && pattern[i + 1] == '[') {
        sb.append("%["); i += 2
        continue
      }
      if (c == '[' && (magic == Magic.MAGIC || magic == Magic.VERY_MAGIC)) {
        i = handleOpener(pattern, i, 1, sb)
        continue
      }
      sb.append(c)
      i++
    }
    return sb.toString()
  }

  /**
   * Handles a collection opener starting at [openerStart] with the given [openerLen]
   * (1 for '[', 2 for '\['). Appends the (possibly transformed) result to [sb] and returns
   * the index of the next character to process.
   */
  private fun handleOpener(pattern: String, openerStart: Int, openerLen: Int, sb: StringBuilder): Int {
    val contentStart = openerStart + openerLen
    val closeIndex = findCollectionEnd(pattern, contentStart)
    if (closeIndex == -1) {
      // No matching ']': the opening bracket is a literal '['.
      return if (openerLen == 1) {
        // A magic-mode '[' becomes '\['.
        sb.append("\\[")
        openerStart + 1
      } else {
        // A no-magic '\[' becomes a bare '[', which is literal in no-magic modes.
        sb.append("[")
        openerStart + 2
      }
    }

    // Valid collection. Determine whether it starts with a literal ']'.
    var firstMember = contentStart
    if (firstMember < pattern.length && pattern[firstMember] == '^') firstMember++
    val leadingBracket = if (firstMember < pattern.length && pattern[firstMember] == ']') firstMember else -1

    sb.append(pattern, openerStart, contentStart)
    for (j in contentStart until closeIndex) {
      if (j == leadingBracket) sb.append("\\]") else sb.append(pattern[j])
    }
    sb.append(']')
    return closeIndex + 1
  }

  /**
   * Scans a collection body starting at [start] (the character after the opener) and
   * returns the index of the matching ']', or -1 if there is none. Mirrors Vim's
   * `skip_anyof`: a leading ']' or '-' is a literal member, in-range backslash escapes and
   * `[:class:]` expressions are skipped over.
   */
  private fun findCollectionEnd(pattern: String, start: Int): Int {
    var p = start
    if (p < pattern.length && pattern[p] == '^') p++
    if (p < pattern.length && (pattern[p] == ']' || pattern[p] == '-')) p++
    while (p < pattern.length && pattern[p] != ']') {
      val c = pattern[p]
      when {
        c == '-' -> {
          p++
          if (p < pattern.length && pattern[p] != ']') p++
        }

        c == '\\' && p + 1 < pattern.length && IN_RANGE_ESCAPES.indexOf(pattern[p + 1]) >= 0 -> p += 2

        c == '[' -> {
          val after = skipCharClassExpression(pattern, p)
          p = if (after > p) after else p + 1
        }

        else -> p++
      }
    }
    return if (p < pattern.length && pattern[p] == ']') p else -1
  }

  /**
   * If [start] points at the beginning of a character class expression ("[:name:]",
   * "[=x=]" or "[.x.]"), returns the index just past its closing ']'. Otherwise returns
   * [start] unchanged.
   */
  private fun skipCharClassExpression(pattern: String, start: Int): Int {
    if (start + 1 >= pattern.length) return start
    val delimiter = pattern[start + 1]
    if (delimiter != ':' && delimiter != '=' && delimiter != '.') return start
    var p = start + 2
    while (p + 1 < pattern.length) {
      if (pattern[p] == delimiter && pattern[p + 1] == ']') return p + 2
      p++
    }
    return start
  }
}
