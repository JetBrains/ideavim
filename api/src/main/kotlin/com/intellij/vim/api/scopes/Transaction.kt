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
import com.intellij.vim.api.Highlighter
import com.intellij.vim.api.TextInfo
import com.intellij.vim.api.TextSelectionType
import com.intellij.vim.api.scopes.caret.CaretTransaction

@VimPluginDsl
interface Transaction {
  fun forEachCaret(block: CaretTransaction.() -> Unit)

  fun <T> mapEachCaret(block: CaretTransaction.() -> T): List<T>

  fun forEachCaretSorted(block: CaretTransaction.() -> Unit)

  fun withCaret(caretId: CaretId, block: CaretTransaction.() -> Unit)

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
  fun insertText(
    caretId: CaretId,
    position: Int,
    text: String,
    caretAfterInsertedText: Boolean = true,
    preserveIndentation: Boolean = true
  ): Boolean

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
    textInfo: TextInfo,
    selectionType: TextSelectionType = TextSelectionType.CHARACTER_WISE,
    preserveIndentation: Boolean = true
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
  ): Boolean

  fun addCaret(offset: Int): CaretId
  fun removeCaret(caretId: CaretId)

  // Highlighting
  fun addHighlighter(startOffset: Int, endOffset: Int, backgroundColor: Color?, foregroundColor: Color?): Highlighter
  fun removeHighlighter(highlighter: Highlighter)
  fun removeHighlighters(highlighters: List<Highlighter>)
}
