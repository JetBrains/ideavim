/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.editor.Editor

/**
 * Moves the last added cursor to the next occurrence
 */
internal class SkipOccurrenceHandler(private val caseSensitivity: CaseSensitivity) : WriteActionHandler() {

  override fun executeInWriteAction(editor: Editor): Boolean {
    val primaryCaret = editor.caretModel.primaryCaret
    val text = primaryCaret.selectedText ?: return false

    val search = editor.occurrenceSearch
    val wholeWord = search?.wholeWord ?: false
    val ignoreCase = search?.ignoreCase ?: caseSensitivity.ignoresCase(text)

    val nextOffset = findNextOccurrence(editor, primaryCaret.offset, text, wholeWord, ignoreCase) ?: return false
    if (editor.isSelectedByAnyCaret(nextOffset)) {
      showNoMoreMatches()
      return false
    }

    primaryCaret.moveToVisualPosition(editor.offsetToVisualPosition(nextOffset))
    val occurrence = primaryCaret.selectOccurrence(nextOffset, text)
    editor.occurrenceSearch = search?.copy(lastOccurrence = occurrence)
    return true
  }
}
