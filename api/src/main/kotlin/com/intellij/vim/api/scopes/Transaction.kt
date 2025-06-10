/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.scopes.caret.CaretTransaction

@VimPluginDsl
interface Transaction {
  fun forEachCaret(block: CaretTransaction.() -> Unit)

  fun <T> mapEachCaret(block: CaretTransaction.() -> T): List<T>

  fun forEachCaretSorted(block: CaretTransaction.() -> Unit)

  fun withCaret(caretId: CaretId, block: CaretTransaction.() -> Unit)

  fun insertText(caretId: CaretId, atPosition: Int, text: CharSequence)
  fun deleteText(startOffset: Int, endOffset: Int)
  fun replaceTextBlockwise(caretId: CaretId, startOffset: Int, endOffset: Int, text: List<String>)
  fun replaceText(caretId: CaretId, startOffset: Int, endOffset: Int, text: String)

  /**
   * Inserts text before the caret position.
   * Caret will be placed after the inserted text.
   *
   * @param caretId The ID of the caret to use for insertion
   * @param position The position where text will be inserted
   * @param text The text to insert
   * @param options Optional parameters for the insertion operation
   * @return True if the operation was successful, false otherwise
   */
  fun insertTextBeforeCaret(
    caretId: CaretId,
    position: Int,
    text: String,
    options: TextOperationOptions = TextOperationOptions(),
  ): Boolean

  /**
   * Inserts text after the caret position.
   * Caret will be placed before the inserted text.
   *
   * @param caretId The ID of the caret to use for insertion
   * @param position The position where text will be inserted
   * @param text The text to insert
   * @param options Optional parameters for the insertion operation
   * @return True if the operation was successful, false otherwise
   */
  fun insertTextAfterCaret(
    caretId: CaretId,
    position: Int,
    text: String,
    options: TextOperationOptions = TextOperationOptions(),
  ): Boolean

  /**
   * Inserts text at the specified line.
   *
   * @param caretId The ID of the caret to use for insertion
   * @param line The line number where text will be inserted
   * @param text The text to insert
   * @param options Optional parameters for the insertion operation
   */
  fun insertTextAtLine(
    caretId: CaretId,
    line: Int,
    text: String,
    options: TextOperationOptions = TextOperationOptions(),
  )

  /**
   * Replaces text between startOffset and endOffset.
   * This method handles caret positioning and mark updates.
   *
   * @param caretId The ID of the caret to use for replacement
   * @param startOffset The start offset of the text to replace
   * @param endOffset The end offset of the text to replace
   * @param text The text to replace with
   * @param options Optional parameters for the replacement operation
   * @return True if the operation was successful, false otherwise
   */
  fun replaceText(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: String,
    options: TextOperationOptions = TextOperationOptions(),
  ): Boolean

  /**
   * Deletes text between startOffset and endOffset.
   * This method handles caret positioning and mark updates.
   *
   * @param caretId The ID of the caret to use for deletion
   * @param startOffset The start offset of the text to delete
   * @param endOffset The end offset of the text to delete
   * @param options Optional parameters for the deletion operation
   * @return True if the operation was successful, false otherwise
   */
  fun deleteText(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    options: DeleteOptions = DeleteOptions(),
  ): Boolean

  /**
   * Options for text operations like insertion and replacement.
   *
   * @property count Number of times to repeat the operation
   * @property updateVisualMarks Whether to update visual marks after the operation
   * @property rawIndent Whether to use raw indentation
   * @property modifyRegister Whether to modify the register
   * @property caretAfterText Whether to place the caret after the inserted/replaced text
   */
  data class TextOperationOptions(
    val count: Int = 1,
    val updateVisualMarks: Boolean = false,
    val rawIndent: Boolean = false,
    val modifyRegister: Boolean = false,
    val caretAfterText: Boolean = true,
  )

  /**
   * Options for delete operations.
   *
   * @property updateVisualMarks Whether to update visual marks after deletion
   * @property isChange Whether this deletion is part of a change operation
   * @property saveToRegister Whether to save the deleted text to a register
   */
  data class DeleteOptions(
    val updateVisualMarks: Boolean = false,
    val isChange: Boolean = false,
    val saveToRegister: Boolean = false,
  )

  fun addCaret(offset: Int): CaretId
  fun removeCaret(caretId: CaretId)
}
