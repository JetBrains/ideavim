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
}