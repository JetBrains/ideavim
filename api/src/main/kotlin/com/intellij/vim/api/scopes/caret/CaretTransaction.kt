/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.caret

import com.intellij.vim.api.Line
import com.intellij.vim.api.Range

interface CaretTransaction: CaretRead {
  suspend fun updateCaret(offset: Int, selection: Range.Simple? = null)

  suspend fun insertText(
    position: Int,
    text: String,
    caretAfterInsertedText: Boolean = true
  ): Boolean

  suspend fun replaceText(
    startOffset: Int,
    endOffset: Int,
    text: String,
  ): Boolean

  suspend fun replaceTextBlockwise(
    range: Range.Block,
    text: List<String>
  )

  suspend fun deleteText(
    startOffset: Int,
    endOffset: Int,
  ): Boolean

  // temporary
  suspend fun getLineStartOffset(line: Int): Int
  suspend fun getLineEndOffset(line: Int, allowEnd: Boolean): Int
  suspend fun getLine(offset: Int): Line

  /**
   * Adds a jump with the current caret's position to the jump list.
   *
   * @param reset Whether to reset the current position in the jump list
   */
  suspend fun addJump(reset: Boolean)

  /**
   * Saves the location of the current caret to the jump list and sets the ' mark.
   */
  // todo: maybe not necessary
  suspend fun saveJumpLocation()
}