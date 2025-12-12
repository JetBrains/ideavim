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
import com.intellij.vim.api.scopes.editor.EditorAccessor

/**
 * Scope for caret operations that should be executed under the write lock.
 */
@VimApiDsl
interface CaretTransaction : CaretRead, EditorAccessor {
  /**
   * Updates the caret position.
   *
   * This function is analogous to Vim's `cursor()` function.
   *
   * If there is an active selection, it will be extended from the anchor to the new offset.
   * If there is no selection, the caret simply moves to the new offset without creating one.
   *
   * @param offset The new offset (position) for the caret.
   *               Valid range is [0, fileSize) for modes that don't allow the caret after the last character
   *               (e.g., normal mode), or [0, fileSize] for modes that allow it (e.g., insert mode).
   * @throws IllegalArgumentException If the offset is outside the valid range for the current mode.
   *                                  The caret position remains unchanged when an exception is thrown.
   */
  fun updateCaret(offset: Int)

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
  fun insertText(
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
  fun replaceText(
    startOffset: Int,
    endOffset: Int,
    text: String,
  ): Boolean

  /**
   * Replaces text in multiple ranges (blocks) with new text.
   *
   * This function performs a blockwise replacement, replacing each line in the block
   * with the corresponding string from the text list. The number of replacement strings
   * must match the number of lines in the block.
   *
   * @param range A block range defined by start and end offsets
   * @param text A list of strings to replace each line in the block
   * @throws IllegalArgumentException If the size of the text list doesn't match the number of lines in the block,
   *                                 or if any range in the block is invalid
   */
  fun replaceTextBlockwise(
    range: Range.Block,
    text: List<String>,
  )

  /**
   * Replaces text in multiple ranges (blocks) with a single text.
   *
   * This function performs a blockwise replacement, replacing each line in the block
   * with the same text string.
   *
   * @param range A block range defined by start and end offsets
   * @param text The text to replace each line in the block with
   * @throws IllegalArgumentException If any range in the block is invalid
   */
  fun replaceTextBlockwise(
    range: Range.Block,
    text: String,
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
  fun deleteText(
    startOffset: Int,
    endOffset: Int,
  ): Boolean

  /**
   * Adds a jump with the current caret's position to the jump list.
   *
   * @param reset Whether to reset the current position in the jump list
   */
  fun addJump(reset: Boolean)

  /**
   * Saves the location of the current caret to the jump list and sets the ' mark.
   */
  fun saveJumpLocation()
}
