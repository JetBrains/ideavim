/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.editor

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Color
import com.intellij.vim.api.HighlightId
import com.intellij.vim.api.Jump
import com.intellij.vim.api.scopes.VimPluginDsl
import com.intellij.vim.api.scopes.editor.caret.CaretTransaction

/**
 * Scope for editor functions that should be executed under write lock.
 */
@VimPluginDsl
interface Transaction: Read {
  /**
   * Executes the provided block for each caret in the editor and returns a list of results.
   *
   * Example usage:
   * ```kotlin
   * editor {
   *   change {
   *     forEachCaret {
   *       // Perform operations on each caret
   *     }
   *   }
   * }
   * ```
   *
   * @param block The block to execute for each caret
   * @return A list containing the results of executing the block for each caret
   */
  suspend fun <T> forEachCaret(block: suspend CaretTransaction.() -> T): List<T>

  /**
   * Executes the provided block with a specific caret as the receiver.
   *
   * This function allows you to perform write operations on a specific caret identified by its ID.
   *
   * Example usage:
   * ```kotlin
   * editor {
   *   change {
   *     val caretId = caretIds.first() // Get the ID of the first caret
   *     with(caretId) {
   *       // Perform operations on the specific caret
   *       deleteText(offset, offset + 5)
   *       updateCaret(newOffset)
   *     }
   *   }
   * }
   * ```
   *
   * @param caretId The ID of the caret to use
   * @param block The block to execute with the specified caret as the receiver
   */
  suspend fun <T> with(caretId: CaretId, block: suspend CaretTransaction.() -> T): T

  /**
   * Executes the provided block with the primary caret as the receiver.
   *
   * This function allows you to perform write operations on the primary caret in the editor.
   *
   * Example usage:
   * ```kotlin
   * editor {
   *   change {
   *     withPrimaryCaret {
   *       // Perform operations on the primary caret
   *       deleteText(offset, offset + 5)
   *       updateCaret(newOffset)
   *     }
   *   }
   * }
   * ```
   *
   * @param block The block to execute with the primary caret as the receiver
   */
  suspend fun <T> withPrimaryCaret(block: suspend CaretTransaction.() -> T): T

  /**
   * Adds a new caret at the specified offset in the editor.
   *
   * @param offset The offset at which to add the caret
   * @return The ID of the newly created caret if successful, null otherwise
   * @throws IllegalArgumentException if offset is not in valid range `[0, fileLength - 1]`
   */
  suspend fun addCaret(offset: Int): CaretId?

  /**
   * Removes a caret from the editor.
   *
   * @param caretId The ID of the caret to remove
   * @throws IllegalArgumentException if caret with [caretId] is not found
   */
  suspend fun removeCaret(caretId: CaretId)

  /**
   * Adds a highlight to the editor.
   *
   * @param startOffset The start offset of the highlight
   * @param endOffset The end offset of the highlight
   * @param backgroundColor The background color of the highlight, or null for no background color
   * @param foregroundColor The foreground color of the highlight, or null for no foreground color
   * @return The ID of the newly created highlight
   */
  suspend fun addHighlight(startOffset: Int, endOffset: Int, backgroundColor: Color?, foregroundColor: Color?): HighlightId

  /**
   * Removes a highlight from the editor.
   *
   * @param highlightId The ID of the highlight to remove
   */
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
  suspend fun addJump(jump: Jump, reset: Boolean = false)

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