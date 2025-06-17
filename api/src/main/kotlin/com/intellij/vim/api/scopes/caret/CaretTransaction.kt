/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.caret

import com.intellij.vim.api.Range

interface CaretTransaction: CaretRead {
  fun updateCaret(offset: Int, selection: Range? = null)

  fun insertText(
    position: Int,
    text: String,
    caretAfterInsertedText: Boolean = true
  ): Boolean


  fun replaceText(
    startOffset: Int,
    endOffset: Int,
    text: String
  ): Boolean

  fun deleteText(
    startOffset: Int,
    endOffset: Int,
  ): Boolean

  // temporary
  fun getLineStartOffset(line: Int): Int
  fun getLineEndOffset(line: Int, allowEnd: Boolean): Int
  fun getLineNumber(offset: Int): Int
}