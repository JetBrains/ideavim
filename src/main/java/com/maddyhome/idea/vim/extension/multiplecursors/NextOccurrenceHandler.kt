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
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.newapi.vim

/**
 * What happens to a selection that is not the last added occurrence, e.g. one made by the user
 */
internal enum class NewSelection {
  /**
   * vim-multiple-cursors: a selection over several lines becomes a cursor on each line, otherwise the next occurrence
   * is added
   */
  SPLIT_LINES_OR_ADD_NEXT,

  /**
   * Find Subword Under of vim-visual-multi: the selection becomes the first region
   */
  START_SEARCH,

  /**
   * Find Next of vim-visual-multi
   */
  ADD_NEXT,
}

/**
 * In Normal mode, selects the word under the caret. In Visual mode, adds a cursor at the next occurrence of the
 * selection, or at the previous one when searching backwards.
 *
 * @param wholeWord Whether the word selected in Normal mode is searched with word boundaries. In Visual mode, the flag
 *   of the started search is used.
 */
internal class NextOccurrenceHandler(
  private val wholeWord: Boolean,
  private val caseSensitivity: CaseSensitivity,
  override val acceptsCount: Boolean = false,
  private val newSelection: NewSelection = NewSelection.SPLIT_LINES_OR_ADD_NEXT,
  private val direction: Direction = Direction.FORWARDS,
) : WriteActionHandler() {

  override fun executeInWriteAction(editor: Editor): Boolean =
    if (editor.inVisualMode) addNextOccurrence(editor) else startWithWordUnderCaret(editor)

  private fun startWithWordUnderCaret(editor: Editor): Boolean {
    if (editor.caretModel.caretCount > 1) return false

    editor.occurrenceSearch = null
    val word = editor.caretModel.primaryCaret.selectWordUnderCaret() ?: return false
    startOccurrenceSearch(editor, word, wholeWord, caseSensitivity)
    return true
  }

  private fun addNextOccurrence(editor: Editor): Boolean {
    val carets = editor.caretModel.allCarets
    if (carets.any { it.selectedText == null }) return false
    if (!isLastOccurrenceSelected(editor)) {
      when (newSelection) {
        NewSelection.SPLIT_LINES_OR_ADD_NEXT -> if (splitMultilineSelections(editor)) return true
        NewSelection.START_SEARCH -> {
          val selection = editor.caretModel.primaryCaret.selectedRange
          startOccurrenceSearch(editor, selection, wholeWord = false, caseSensitivity)
          return true
        }
        NewSelection.ADD_NEXT -> Unit
      }
    }
    // Cursors added in another way may select different texts
    if (!selectSameText(editor, carets)) return false

    val search = continueOrStartSearch(editor).copy(direction = direction)
    editor.occurrenceSearch = search
    val current = search.lastOccurrence
    val pattern = editor.vim.getText(current)

    val nextOffset = findOccurrence(editor, current, pattern, search.wholeWord, search.ignoreCase, direction)
    if (nextOffset == null || editor.isSelectedByAnyCaret(nextOffset)) {
      showNoMoreMatches()
      return false
    }

    val caret = editor.caretModel.addCaret(editor.offsetToVisualPosition(nextOffset), true) ?: return false
    editor.updateCaretsVisualAttributes()
    editor.occurrenceSearch = search.copy(lastOccurrence = caret.selectOccurrence(nextOffset, pattern))
    return true
  }

  private fun isLastOccurrenceSelected(editor: Editor): Boolean =
    editor.occurrenceSearch?.lastOccurrence == editor.caretModel.primaryCaret.selectedRange

  /**
   * A new search, e.g. for a selection made by the user, is without word boundaries
   */
  private fun continueOrStartSearch(editor: Editor): OccurrenceSearch {
    val selection = editor.caretModel.primaryCaret.selectedRange
    return editor.occurrenceSearch?.takeIf { it.lastOccurrence == selection }
      ?: startOccurrenceSearch(editor, selection, wholeWord = false, caseSensitivity)
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

    leaveVisualMode(editor.vim)
    newCaretPositions.forEach { editor.caretModel.addCaret(it, true) }
    editor.updateCaretsVisualAttributes()
    return true
  }

  private fun positionsOnFollowingLines(editor: Editor, caret: Caret): List<VisualPosition> {
    // A selection that ends with a new line ends on the next line
    val endsWithNewLine = caret.selectedText?.endsWith('\n') == true
    val selectionEnd = if (endsWithNewLine) caret.selectionEnd - 1 else caret.selectionEnd
    val document = editor.document
    val followingLines = document.getLineNumber(selectionEnd) - document.getLineNumber(caret.selectionStart)

    val start = editor.offsetToVisualPosition(caret.selectionStart)
    return (1..followingLines).map { VisualPosition(start.line + it, start.column) }
  }
}
