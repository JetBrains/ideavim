/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Color
import com.intellij.vim.api.HighlightId
import com.intellij.vim.api.scopes.caret.CaretTransaction

@VimPluginDsl
interface Transaction {
  fun <T> forEachCaret(block: CaretTransaction.() -> T): List<T>
  fun with(caretId: CaretId, block: CaretTransaction.() -> Unit)

  fun addCaret(offset: Int): CaretId
  fun removeCaret(caretId: CaretId)

  // Highlighting
  fun addHighlight(startOffset: Int, endOffset: Int, backgroundColor: Color?, foregroundColor: Color?): HighlightId
  fun removeHighlight(highlightId: HighlightId)

  /**
   * Sets a mark at the current position for each caret in the editor.
   *
   * @param char The character key of the mark (a-z, A-Z, etc.)
   * @return True if the mark was successfully set, false otherwise
   */
  fun setMark(char: Char): Boolean

  /**
   * Removes a mark for all carets in the editor.
   *
   * @param char The character key of the mark to remove (a-z, A-Z, etc.)
   */
  fun removeMark(char: Char)

  /**
   * Sets a global mark at the current position.
   *
   * @param char The character key of the mark (A-Z)
   * @return True if the mark was successfully set, false otherwise
   */
  fun setGlobalMark(char: Char): Boolean

  /**
   * Removes a global mark.
   *
   * @param char The character key of the mark to remove (A-Z)
   */
  fun removeGlobalMark(char: Char)

  /**
   * Sets a global mark at the specified offset.
   *
   * @param char The character key of the mark (A-Z)
   * @param offset The offset to set the mark to
   * @return True if the mark was successfully set, false otherwise
   */
  fun setGlobalMark(char: Char, offset: Int): Boolean

  /**
   * Resets all marks.
   *
   * This removes all marks, both global and local.
   */
  fun resetAllMarks()
}
