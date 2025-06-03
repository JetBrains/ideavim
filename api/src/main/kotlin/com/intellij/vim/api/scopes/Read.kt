/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.CaretData
import com.intellij.vim.api.CaretId
import com.intellij.vim.api.scopes.caret.CaretRead

@VimPluginDsl
interface Read {
  fun forEachCaret(block: CaretRead.() -> Unit)

  fun <T> mapEachCaret(block: CaretRead.() -> T): List<T>

  fun forEachCaretSorted(block: CaretRead.() -> Unit)

  fun withCaret(caretId: CaretId, block: CaretRead.() -> Unit)

  fun getLineStartOffset(line: Int): Int
  fun getLineEndOffset(line: Int, allowEnd: Boolean): Int
  fun getAllCaretsData(): List<CaretData>
  fun getAllCaretsDataSortedByOffset(): List<CaretData>
  fun getAllCaretIds(): List<CaretId>
  fun getAllCaretIdsSortedByOffset(): List<CaretId>
}