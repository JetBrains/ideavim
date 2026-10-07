/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.SearchOptions
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.userData
import com.maddyhome.idea.vim.newapi.vim

/**
 * The search that adds the cursors, one occurrence at a time
 *
 * The flags are decided when the search starts, and kept while the next occurrences are added. E.g. `FOO` found for
 * `foo` must not make 'smartcase' case sensitive.
 *
 * @param lastOccurrence The selection of the last added cursor. When the current selection is different, the user has
 *   selected something else, and a new search starts.
 */
internal data class OccurrenceSearch(
  val wholeWord: Boolean,
  val ignoreCase: Boolean,
  val lastOccurrence: TextRange,
)

internal var Editor.occurrenceSearch: OccurrenceSearch? by userData()

/**
 * Returns the start offset of the next occurrence of [text] after [startOffset], wrapping around if 'wrapscan' is set
 */
internal fun findNextOccurrence(
  editor: Editor,
  startOffset: Int,
  text: String,
  wholeWord: Boolean,
  ignoreCase: Boolean,
): Int? {
  val searchOptions = enumSetOf(SearchOptions.WHOLE_FILE)
  if (injector.options(editor.vim).wrapscan) {
    searchOptions.add(SearchOptions.WRAP)
  }
  val pattern = occurrencePattern(text, wholeWord, ignoreCase)
  return injector.searchHelper.findPattern(editor.vim, pattern, startOffset, 1, searchOptions)?.startOffset
}

internal fun findAllOccurrences(editor: Editor, text: String, wholeWord: Boolean, ignoreCase: Boolean): List<TextRange> {
  val pattern = occurrencePattern(text, wholeWord, ignoreCase)
  return injector.searchHelper.findAll(editor.vim, pattern, 0, -1, false)
}

/**
 * The text is matched literally with "very nomagic", where only the backslash needs escaping. The case is forced with
 * `\c` or `\C`, which takes precedence over 'ignorecase' and 'smartcase'.
 */
private fun occurrencePattern(text: String, wholeWord: Boolean, ignoreCase: Boolean): String {
  val escapedText = text.replace("\\", "\\\\")
  val case = if (ignoreCase) "\\c" else "\\C"
  val body = if (wholeWord) "\\<$escapedText\\>" else escapedText
  return "\\V$case$body"
}
