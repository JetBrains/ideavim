/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.key.AbbreviationEntry

open class VimAbbreviationGroupBase : VimAbbreviationGroup {
  private val entriesByMode: MutableMap<MappingMode, MutableMap<String, AbbreviationEntry>> = mutableMapOf()

  override fun setAbbreviation(lhs: String, rhs: String, modes: Set<MappingMode>, recursive: Boolean) {
    requireValidAbbreviationLhs(lhs)
    val entry = AbbreviationEntry(lhs, rhs, modes, recursive)
    modes.forEach { mode ->
      entriesByMode.getOrPut(mode) { mutableMapOf() }[lhs] = entry
    }
  }

  override fun getAbbreviation(lhs: String, mode: MappingMode): AbbreviationEntry? =
    entriesByMode[mode]?.get(lhs)

  override fun removeAllAbbreviations() {
    entriesByMode.clear()
  }

  private fun requireValidAbbreviationLhs(lhs: String) {
    if (!isValidAbbreviationLhs(lhs)) {
      throw exExceptionMessage("E474.arg", lhs)
    }
  }

  /** Vim accepts only the three lhs shapes (full-id, end-id, non-id) defined in `:help abbreviations`. */
  private fun isValidAbbreviationLhs(lhs: String): Boolean {
    if (lhs.isEmpty()) return false
    if (lhs.any(Char::isWhitespace)) return false
    return isFullId(lhs) || isEndId(lhs) || isNonId(lhs)
  }

  private fun isFullId(lhs: String): Boolean = lhs.all(::isKeywordChar)

  private fun isEndId(lhs: String): Boolean =
    isKeywordChar(lhs.last()) && lhs.dropLast(1).none(::isKeywordChar)

  private fun isNonId(lhs: String): Boolean = !isKeywordChar(lhs.last())

  private fun isKeywordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'
}
