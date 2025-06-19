/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Color
import com.intellij.vim.api.HighlightId
import com.intellij.vim.api.scopes.Transaction
import com.intellij.vim.api.scopes.caret.CaretTransaction
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner

class TransactionImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : Transaction {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  override fun <T> forEachCaret(block: CaretTransaction.() -> T): List<T> {
    return vimEditor.sortedCarets().map { caret -> CaretTransactionImpl(listenerOwner, mappingOwner, caret.caretId).block() }
  }

  override fun with(
    caretId: CaretId,
    block: CaretTransaction.() -> Unit,
  ) {
    vimEditor.carets().find { it.id == caretId.id }
      ?.let { caret -> block(CaretTransactionImpl(listenerOwner, mappingOwner, caret.caretId)) } ?: return
  }

  override fun addCaret(offset: Int): CaretId {
    TODO("Not yet implemented")
  }

  override fun removeCaret(caretId: CaretId) {
    TODO("Not yet implemented")
  }

  override fun addHighlight(
    startOffset: Int,
    endOffset: Int,
    backgroundColor: Color?,
    foregroundColor: Color?,
  ): HighlightId {
    return injector.highlightingService.addHighlighter(
      vimEditor,
      startOffset,
      endOffset,
      backgroundColor,
      foregroundColor
    )
  }

  override fun removeHighlight(highlightId: HighlightId) {
    injector.highlightingService.removeHighlighter(vimEditor, highlightId)
  }
}
