/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.AbbreviationEntry

/**
 * Storage and lookup of Vim abbreviations (`:abbrev`, `:iabbrev`, `:cabbrev`).
 *
 * Abbreviations are stored separately from key mappings — they're matched on word-boundary
 * suffix lookup, not keystroke-prefix lookup, so the underlying data structure is different.
 */
interface VimAbbreviationGroup {
  /** Add or replace an abbreviation for the given set of modes. */
  fun setAbbreviation(lhs: String, rhs: String, modes: Set<MappingMode>, recursive: Boolean)

  /** Return the abbreviation registered for [lhs] in [mode], or null. */
  fun getAbbreviation(lhs: String, mode: MappingMode): AbbreviationEntry?

  /** Remove every abbreviation in every mode. */
  fun removeAllAbbreviations()
}
