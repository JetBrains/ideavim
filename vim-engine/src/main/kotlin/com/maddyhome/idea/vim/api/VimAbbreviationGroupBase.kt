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

private typealias EntriesByMode = MutableMap<MappingMode, MutableMap<String, AbbreviationEntry>>

private val BUFFER_LOCAL_ABBREVIATIONS: Key<EntriesByMode> = Key("abbreviation.bufferLocal")

open class VimAbbreviationGroupBase : VimAbbreviationGroup {
  private val globalEntriesByMode: EntriesByMode = mutableMapOf()

  override fun setAbbreviation(lhs: String, rhs: String, modes: Set<MappingMode>, recursive: Boolean) {
    requireValidAbbreviationLhs(lhs)
    storeEntry(globalEntriesByMode, lhs, rhs, modes, recursive)
  }

  override fun setBufferLocalAbbreviation(
    lhs: String,
    rhs: String,
    modes: Set<MappingMode>,
    recursive: Boolean,
    editor: VimEditor,
  ) {
    requireValidAbbreviationLhs(lhs)
    storeEntry(bufferLocalEntries(editor), lhs, rhs, modes, recursive)
  }

  override fun getAbbreviation(lhs: String, mode: MappingMode, editor: VimEditor): AbbreviationEntry? =
    bufferLocalEntriesIfPresent(editor)?.get(mode)?.get(lhs)
      ?: globalEntriesByMode[mode]?.get(lhs)

  override fun removeAbbreviation(lhs: String, modes: Set<MappingMode>) {
    modes.forEach { mode -> globalEntriesByMode[mode]?.remove(lhs) }
  }

  override fun removeBufferLocalAbbreviation(lhs: String, modes: Set<MappingMode>, editor: VimEditor) {
    val entries = bufferLocalEntriesIfPresent(editor) ?: return
    modes.forEach { mode -> entries[mode]?.remove(lhs) }
  }

  override fun clearAbbreviations(modes: Set<MappingMode>) {
    modes.forEach { mode -> globalEntriesByMode.remove(mode) }
  }

  override fun clearBufferLocalAbbreviations(modes: Set<MappingMode>, editor: VimEditor) {
    val entries = bufferLocalEntriesIfPresent(editor) ?: return
    modes.forEach { mode -> entries.remove(mode) }
  }

  override fun listAbbreviations(
    modes: Set<MappingMode>,
    editor: VimEditor,
    bufferLocalOnly: Boolean,
  ): List<AbbreviationListing> {
    val result = mutableListOf<AbbreviationListing>()
    val buffer = bufferLocalEntriesIfPresent(editor)
    modes.forEach { mode ->
      buffer?.get(mode)?.values?.forEach { result.add(AbbreviationListing(it, mode, bufferLocal = true)) }
      if (!bufferLocalOnly) {
        globalEntriesByMode[mode]?.values?.forEach { result.add(AbbreviationListing(it, mode, bufferLocal = false)) }
      }
    }
    return result.sortedBy { it.entry.lhs }
  }

  private fun bufferLocalEntries(editor: VimEditor): EntriesByMode =
    injector.vimStorageService.getOrPutBufferData(editor, BUFFER_LOCAL_ABBREVIATIONS) { mutableMapOf() }

  private fun bufferLocalEntriesIfPresent(editor: VimEditor): EntriesByMode? =
    injector.vimStorageService.getDataFromBuffer(editor, BUFFER_LOCAL_ABBREVIATIONS)

  private fun storeEntry(
    target: EntriesByMode,
    lhs: String,
    rhs: String,
    modes: Set<MappingMode>,
    recursive: Boolean,
  ) {
    val entry = AbbreviationEntry(lhs, rhs, modes, recursive)
    modes.forEach { mode -> target.getOrPut(mode) { mutableMapOf() }[lhs] = entry }
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
