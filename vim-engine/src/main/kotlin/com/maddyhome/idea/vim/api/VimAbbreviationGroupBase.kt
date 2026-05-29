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
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper.isKeyword
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext

private typealias EntriesByMode = MutableMap<MappingMode, MutableMap<String, AbbreviationEntry>>

private val BUFFER_LOCAL_ABBREVIATIONS: Key<EntriesByMode> = Key("abbreviation.bufferLocal")

open class VimAbbreviationGroupBase : VimAbbreviationGroup {
  private val globalEntriesByMode: EntriesByMode = mutableMapOf()

  override fun setAbbreviation(
    abbrev: AbbreviationEntry,
    editor: VimEditor,
  ) {
    requireValidAbbreviationLhs(editor, abbrev.lhs)
    storeEntry(globalEntriesByMode, abbrev)
  }

  override fun setBufferLocalAbbreviation(
    abbrev: AbbreviationEntry,
    editor: VimEditor,
  ) {
    requireValidAbbreviationLhs(editor, abbrev.lhs)
    storeEntry(bufferLocalEntries(editor), abbrev)
  }

  override fun resolveAbbreviation(lhs: String, mode: MappingMode, editor: VimEditor): String? {
    val entry = lookupEntry(lhs, mode, editor) ?: return null
    return if (entry.expression) evaluateExpression(entry, editor) else entry.rhs
  }

  private fun lookupEntry(lhs: String, mode: MappingMode, editor: VimEditor): AbbreviationEntry? =
    bufferLocalEntriesIfPresent(editor)?.get(mode)?.get(lhs)
      ?: globalEntriesByMode[mode]?.get(lhs)

  private fun evaluateExpression(entry: AbbreviationEntry, editor: VimEditor): String? {
    val expression = injector.vimscriptParser.parseExpression(entry.rhs)
    if (expression == null) {
      injector.messages.showErrorMessage(editor, injector.messages.message("E15", entry.rhs))
      return null
    }
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    return try {
      expression.evaluate(editor, context, CommandLineVimLContext).toOutputString()
    } catch (e: Exception) {
      injector.messages.showErrorMessage(editor, e.message ?: injector.messages.message("E15", entry.rhs))
      null
    }
  }

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
      buffer?.get(mode)?.values?.forEach { result.add(it.toListing(mode, bufferLocal = true)) }
      if (!bufferLocalOnly) {
        globalEntriesByMode[mode]?.values?.forEach { result.add(it.toListing(mode, bufferLocal = false)) }
      }
    }
    return result.sortedBy { it.lhs }
  }

  private fun AbbreviationEntry.toListing(mode: MappingMode, bufferLocal: Boolean): AbbreviationListing =
    AbbreviationListing(lhs, rhs, mode, bufferLocal, isExpression = expression)

  private fun bufferLocalEntries(editor: VimEditor): EntriesByMode =
    injector.vimStorageService.getOrPutBufferData(editor, BUFFER_LOCAL_ABBREVIATIONS) { mutableMapOf() }

  private fun bufferLocalEntriesIfPresent(editor: VimEditor): EntriesByMode? =
    injector.vimStorageService.getDataFromBuffer(editor, BUFFER_LOCAL_ABBREVIATIONS)

  private fun storeEntry(
    target: EntriesByMode,
    abbrev: AbbreviationEntry,
  ) {
    abbrev.modes.forEach { mode -> target.getOrPut(mode) { mutableMapOf() }[abbrev.lhs] = abbrev }
  }

  private fun requireValidAbbreviationLhs(editor: VimEditor, lhs: String) {
    if (!isValidAbbreviationLhs(editor, lhs)) {
      throw exExceptionMessage("E474.arg", lhs)
    }
  }

  /** Vim accepts only the three lhs shapes (full-id, end-id, non-id) defined in `:help abbreviations`. */
  private fun isValidAbbreviationLhs(editor: VimEditor, lhs: String): Boolean {
    if (lhs.isEmpty()) return false
    if (lhs.any(Char::isWhitespace)) return false
    return isFullId(editor, lhs) || isEndId(editor, lhs) || isNonId(editor, lhs)
  }

  private fun isFullId(editor: VimEditor, lhs: String): Boolean = lhs.all { isKeyword(editor, it) }

  private fun isEndId(editor: VimEditor, lhs: String): Boolean =
    isKeyword(editor, lhs.last()) && lhs.dropLast(1).none { isKeyword(editor, it) }

  private fun isNonId(editor: VimEditor, lhs: String): Boolean = !isKeyword(editor, lhs.last())
}
