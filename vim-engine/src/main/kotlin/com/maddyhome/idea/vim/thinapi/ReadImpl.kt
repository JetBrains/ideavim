/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretData
import com.intellij.vim.api.CaretId
import com.intellij.vim.api.scopes.Read
import com.intellij.vim.api.scopes.caret.CaretRead
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset

open class ReadImpl(
  private val editor: VimEditor,
  private val context: ExecutionContext,
) : Read, VimScopeImpl(editor, context) {
  override fun forEachCaret(block: CaretRead.() -> Unit) {
    editor.carets().forEach { caret -> CaretReadImpl(caret.caretId, editor, context).block() }
  }

  override fun <T> mapEachCaret(block: CaretRead.() -> T): List<T> {
    return editor.carets().map { caret -> CaretReadImpl(caret.caretId, editor, context).block() }
  }

  override fun forEachCaretSorted(block: CaretRead.() -> Unit) {
    editor.sortedCarets().forEach { caret -> CaretReadImpl(caret.caretId, editor, context).block() }
  }

  override fun withCaret(
    caretId: CaretId,
    block: CaretRead.() -> Unit,
  ) {
    CaretReadImpl(caretId, editor, context).block()
  }

  override fun getLineStartOffset(line: Int): Int {
    return editor.getLineStartOffset(line)
  }

  override fun getLineEndOffset(line: Int, allowEnd: Boolean): Int {
    return editor.getLineEndOffset(line, allowEnd)
  }

  override fun getAllCaretsData(): List<CaretData> {
    return editor.carets().map { caret -> caret.caretId to caret.caretInfo }
  }

  override fun getAllCaretsDataSortedByOffset(): List<CaretData> {
    return editor.sortedCarets().map { caret -> caret.caretId to caret.caretInfo }
  }

  override fun getAllCaretIds(): List<CaretId> {
    return editor.carets().map { caret -> caret.caretId }
  }

  override fun getAllCaretIdsSortedByOffset(): List<CaretId> {
    return editor.sortedCarets().map { caret -> caret.caretId }
  }
}