/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.multiplecursors

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.newapi.vim

/**
 * In Normal mode, selects the word under the caret. In Visual mode, adds a cursor at the next occurrence of the
 * selection, or turns a selection over several lines into a cursor on each line.
 *
 * @param wholeWord Whether the word selected in Normal mode is searched with word boundaries. In Visual mode, the flag
 *   of the started search is used.
 */
internal class NextOccurrenceHandler(
  private val wholeWord: Boolean,
  private val caseSensitivity: CaseSensitivity,
) : WriteActionHandler() {

  override fun executeInWriteAction(editor: Editor) {
    if (editor.inVisualMode) {
      addNextOccurrence(editor)
    } else {
      startWithWordUnderCaret(editor)
    }
  }

  private fun startWithWordUnderCaret(editor: Editor) {
    if (editor.caretModel.caretCount > 1) return

    val word = editor.caretModel.primaryCaret.selectWordUnderCaret()
    editor.occurrenceSearch = word?.let {
      OccurrenceSearch(wholeWord, caseSensitivity.ignoresCase(editor.vim.getText(it)), lastOccurrence = it)
    }
  }

  private fun addNextOccurrence(editor: Editor) {
    val carets = editor.caretModel.allCarets
    if (carets.any { it.selectedText == null }) return
    if (splitMultilineSelections(editor)) return
    // Cursors added in another way may select different texts
    if (!selectSameText(editor, carets)) return

    val search = continueOrStartSearch(editor)
    val pattern = editor.vim.getText(search.lastOccurrence)
    val startOffset = editor.caretModel.primaryCaret.offset

    val nextOffset = findNextOccurrence(editor, startOffset, pattern, search.wholeWord, search.ignoreCase)
    if (nextOffset == null || editor.isSelectedByAnyCaret(nextOffset)) {
      showNoMoreMatches()
      return
    }

    val caret = editor.caretModel.addCaret(editor.offsetToVisualPosition(nextOffset), true) ?: return
    editor.updateCaretsVisualAttributes()
    editor.occurrenceSearch = search.copy(lastOccurrence = caret.selectOccurrence(nextOffset, pattern))
  }

  /**
   * Continues the search if the selection is the last added occurrence. Otherwise, the user has selected something
   * else, which starts a new search without word boundaries.
   */
  private fun continueOrStartSearch(editor: Editor): OccurrenceSearch {
    val selection = editor.caretModel.primaryCaret.selectedRange
    val pattern = editor.vim.getText(selection)
    val previous = editor.occurrenceSearch?.takeIf { it.lastOccurrence == selection }

    val search = OccurrenceSearch(
      wholeWord = previous?.wholeWord ?: false,
      ignoreCase = previous?.ignoreCase ?: caseSensitivity.ignoresCase(pattern),
      lastOccurrence = selection,
    )
    editor.occurrenceSearch = search
    return search
  }

  private fun selectSameText(editor: Editor, carets: List<Caret>): Boolean {
    val ignoreCase = editor.occurrenceSearch?.ignoreCase == true
    return carets.mapNotNull { it.selectedText }.distinctBy { if (ignoreCase) it.lowercase() else it }.size <= 1
  }

  /**
   * Turns each selection over several lines into a cursor at the start column of each of its lines. Returns whether
   * there was any such selection.
   */
  private fun splitMultilineSelections(editor: Editor): Boolean {
    val newCaretPositions = editor.caretModel.allCarets.flatMap { caret ->
      val positions = positionsOnFollowingLines(editor, caret)
      if (positions.isNotEmpty()) {
        caret.vim.moveToOffset(caret.selectionStart)
      }
      positions
    }
    if (newCaretPositions.isEmpty()) return false

    editor.vim.exitVisualMode()
    newCaretPositions.forEach { editor.caretModel.addCaret(it, true) }
    editor.updateCaretsVisualAttributes()
    return true
  }

  private fun positionsOnFollowingLines(editor: Editor, caret: Caret): List<VisualPosition> {
    // A selection that ends with a new line ends on the next line
    val endsWithNewLine = caret.selectedText?.endsWith('\n') == true
    val selectionEnd = if (endsWithNewLine) caret.selectionEnd - 1 else caret.selectionEnd
    val followingLines = editor.document.getLineNumber(selectionEnd) - editor.document.getLineNumber(caret.selectionStart)

    val start = editor.offsetToVisualPosition(caret.selectionStart)
    return (1..followingLines).map { VisualPosition(start.line + it, start.column) }
  }
}
