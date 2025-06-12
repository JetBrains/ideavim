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
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner

class CaretTransactionImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
  override val caretId: CaretId,
) : CaretTransaction, CaretRead by CaretReadImpl(caretId), Read by ReadImpl(listenerOwner, mappingOwner) {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  override fun updateCaret(newInfo: CaretInfo) {
    val caret: VimCaret = vimEditor.carets().find { it.id == caretId.id } ?: return
    caret.moveToOffset(newInfo.offset)
    newInfo.selection?.let { (start, end) ->
      caret.setSelection(start, end)
    } ?: caret.removeSelection()
  }
}