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
 *
 * Two scopes:
 *  * Global — survives across editors and lives until the IDE restarts.
 *  * Buffer-local (`<buffer>`) — bound to the editor's underlying `Document`; gone when the
 *    document is unloaded. Mirrors Vim's `:iabbrev <buffer>`.
 *
 * On lookup, the buffer-local entry wins over the global one for the same lhs, per Vim.
 */
interface VimAbbreviationGroup {
  /** Add or replace a global abbreviation for the given set of modes. */
  fun setAbbreviation(lhs: String, rhs: String, modes: Set<MappingMode>, recursive: Boolean)

  /** Add or replace a buffer-local abbreviation in the [editor]'s document. */
  fun setBufferLocalAbbreviation(
    lhs: String,
    rhs: String,
    modes: Set<MappingMode>,
    recursive: Boolean,
    editor: VimEditor,
  )

  /**
   * Return the effective abbreviation for [lhs] in [mode]: buffer-local first (from [editor]'s
   * document), then global.
   */
  fun getAbbreviation(lhs: String, mode: MappingMode, editor: VimEditor): AbbreviationEntry?

  /** Remove the global abbreviation registered for [lhs] in each of the given [modes]. */
  fun removeAbbreviation(lhs: String, modes: Set<MappingMode>)

  /** Remove the buffer-local abbreviation for [lhs] in each of the given [modes] from [editor]'s document. */
  fun removeBufferLocalAbbreviation(lhs: String, modes: Set<MappingMode>, editor: VimEditor)

  /** Remove every global abbreviation registered in any of the given [modes]. */
  fun clearAbbreviations(modes: Set<MappingMode>)

  /** Remove every buffer-local abbreviation registered in any of the given [modes] in [editor]'s document. */
  fun clearBufferLocalAbbreviations(modes: Set<MappingMode>, editor: VimEditor)

  /**
   * Return every abbreviation defined for any of the given [modes], sorted alphabetically by lhs.
   * If [bufferLocalOnly] is true, only entries scoped to [editor]'s document are returned;
   * otherwise both buffer-local and global entries are returned.
   */
  fun listAbbreviations(modes: Set<MappingMode>, editor: VimEditor, bufferLocalOnly: Boolean): List<AbbreviationListing>
}

/** A single row for `:abbreviate`-family listing output. */
data class AbbreviationListing(
  val entry: AbbreviationEntry,
  val mode: MappingMode,
  val bufferLocal: Boolean,
)
