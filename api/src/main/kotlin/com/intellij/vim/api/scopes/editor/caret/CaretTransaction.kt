/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.editor.caret

import com.intellij.vim.api.models.Range
import com.intellij.vim.api.scopes.VimApiDsl
import com.intellij.vim.api.scopes.editor.Read

/**
 * Scope for caret operations that should be executed under the write lock.
 */
@VimApiDsl
interface CaretTransaction : CaretRead, Read {
  /**
   * Updates the caret position and optionally sets a selection.
   *
   * If a selection is provided, the caret will have this selection after moving to the new offset.
   * If no selection is provided, any existing selection will be removed.
   *
   * The selection range is exclusive, meaning that the character at the end offset is not
   * included in the selection. For example, a selection of (0, 3) would select the first
   * three characters of the text.
   *
   * @param offset The new offset (position) for the caret
   * @param selection Optional selection range
   * @throws IllegalArgumentException If the offset is not in the valid range [0, fileSize),
   *                                 or if the selection range is invalid (start or end out of range,
   *                                 or start > end)
   */
  suspend fun updateCaret(offset: Int, selection: Range.Simple? = null)

  /**
   * Inserts text at the specified position in the document.
   *
   * @param position The position (offset) where the text should be inserted
   *      (a zero-base character offset from the start of the document)
   * @param text The text to insert
   * @param caretAtEnd If true (default), places the caret after on the last character of the inserted text;
   *                              if false, places the caret at the beginning of the inserted text
   * @param insertBeforeCaret If true, inserts the text before the specified position;
   *                    if false (default), inserts the text at the specified position
   * @return true if the insertion was successful, false otherwise
   * @throws IllegalArgumentException If the position is not in the valid range [0, fileSize)
   */
  suspend fun insertText(
    position: Int,
    text: String,
    caretAtEnd: Boolean = true,
    insertBeforeCaret: Boolean = false,
  ): Boolean

  /**
   * Replaces the text between startOffset (inclusive) and endOffset (exclusive)
   * with the specified text. After the operation, the caret is positioned before the last
   * character of the replaced text.
   *
   * @param startOffset The start offset (inclusive) of the text to be replaced
   * @param endOffset The end offset (exclusive) of the text to be replaced
   * @param text The new text to replace the existing text
   * @return true if the replacement was successful, false otherwise
   * @throws IllegalArgumentException If the offsets are not in the valid range [0, fileSize),
   *                                 or if startOffset > endOffset
   */
  suspend fun replaceText(
    startOffset: Int,
    endOffset: Int,
    text: String,
  ): Boolean

  /**
   * Replaces text in multiple ranges (blocks) with new text.
   *
   * This function performs a blockwise replacement, replacing each range in the block
   * with the corresponding string from the text list. The number of replacement strings
   * must match the number of ranges in the block.
   *
   * @param range A block of ranges to be replaced
   * @param text A list of strings to replace each range in the block
   * @throws IllegalArgumentException If the size of the text list doesn't match the number of ranges in the block,
   *                                 or if any range in the block is invalid
   */
  suspend fun replaceTextBlockwise(
    range: Range.Block,
    text: List<String>,
  )

  /**
   * Deletes text between the specified offsets.
   *
   * This function deletes the text between startOffset (inclusive) and endOffset (exclusive).
   * If startOffset equals endOffset, no text is deleted.
   * If startOffset > endOffset, the implementation swaps them and deletes the text between them.
   *
   * @param startOffset The start offset (inclusive) of the text to be deleted
   * @param endOffset The end offset (exclusive) of the text to be deleted
   * @return true if the deletion was successful, false otherwise
   * @throws Exception If endOffset is beyond the file size
   */
  suspend fun deleteText(
    startOffset: Int,
    endOffset: Int,
  ): Boolean

  /**
   * Adds a jump with the current caret's position to the jump list.
   *
   * @param reset Whether to reset the current position in the jump list
   */
  suspend fun addJump(reset: Boolean)

  /**
   * Saves the location of the current caret to the jump list and sets the ' mark.
   */
  suspend fun saveJumpLocation()
}
