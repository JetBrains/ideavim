/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.scopes.Read
import com.intellij.vim.api.scopes.caret.CaretRead
import com.intellij.vim.api.scopes.caret.CaretTransaction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor

class CaretTransactionImpl(
  override val caretId: CaretId,
  private val vimEditor: VimEditor,
  private val context: ExecutionContext,
) : CaretTransaction, CaretRead by CaretReadImpl(caretId, vimEditor, context), Read by ReadImpl(vimEditor, context) {
  override fun updateCaret(newInfo: CaretInfo) {
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return
    caret.moveToOffset(newInfo.offset)
    newInfo.selection?.let { (start, end) ->
      caret.setSelection(start, end)
    } ?: caret.removeSelection()
  }
}