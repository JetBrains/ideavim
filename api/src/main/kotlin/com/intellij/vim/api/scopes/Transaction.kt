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
import com.intellij.vim.api.Jump
import com.intellij.vim.api.scopes.caret.CaretRead
import com.intellij.vim.api.scopes.caret.CaretTransaction

@VimPluginDsl
interface Transaction {
  suspend fun <T> forEachCaret(block: suspend CaretTransaction.() -> T): List<T>
  suspend fun with(caretId: CaretId, block: suspend CaretTransaction.() -> Unit)
  suspend fun withPrimaryCaret(block: suspend CaretTransaction.() -> Unit)

  suspend fun addCaret(offset: Int): CaretId
  suspend fun removeCaret(caretId: CaretId)

  // Highlighting
  suspend fun addHighlight(startOffset: Int, endOffset: Int, backgroundColor: Color?, foregroundColor: Color?): HighlightId
  suspend fun removeHighlight(highlightId: HighlightId)

  /**
   * Sets a mark at the current position for each caret in the editor.
   *
   * @param char The character key of the mark (a-z, A-Z, etc.)
   * @return True if the mark was successfully set, false otherwise
   */
  suspend fun setMark(char: Char): Boolean

  /**
   * Removes a mark for all carets in the editor.
   *
   * @param char The character key of the mark to remove (a-z, A-Z, etc.)
   */
  suspend fun removeMark(char: Char)

  /**
   * Sets a global mark at the current position.
   *
   * @param char The character key of the mark (A-Z)
   * @return True if the mark was successfully set, false otherwise
   */
  suspend fun setGlobalMark(char: Char): Boolean

  /**
   * Removes a global mark.
   *
   * @param char The character key of the mark to remove (A-Z)
   */
  suspend fun removeGlobalMark(char: Char)

  /**
   * Sets a global mark at the specified offset.
   *
   * @param char The character key of the mark (A-Z)
   * @param offset The offset to set the mark to
   * @return True if the mark was successfully set, false otherwise
   */
  suspend fun setGlobalMark(char: Char, offset: Int): Boolean

  /**
   * Resets all marks.
   *
   * This removes all marks, both global and local.
   */
  suspend fun resetAllMarks()

  /**
   * Adds a specific jump to the jump list.
   *
   * @param jump The jump to add
   * @param reset Whether to reset the current position in the jump list
   */
  suspend fun addJump(jump: Jump, reset: Boolean)

  /**
   * Removes a jump from the jump list.
   *
   * @param jump The jump to remove
   */
  suspend fun removeJump(jump: Jump)

  /**
   * Removes the last jump from the jump list.
   */
  suspend fun dropLastJump()

  /**
   * Clears all jumps from the jump list.
   */
  suspend fun clearJumps()
}
