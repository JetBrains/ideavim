/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.transaction

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.VimPluginApi
import com.intellij.vim.api.scopes.read.Read
import com.intellij.vim.api.scopes.read.ReadImpl
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor

internal class TransactionImpl(
  internal val editor: VimEditor,
  internal val context: ExecutionContext,
  internal val vimPluginApi: VimPluginApi,
  internal val readImpl: ReadImpl
): Transaction, Read by readImpl {
  override fun deleteText(startOffset: Int, endOffset: Int) {
    vimPluginApi.deleteText(editor, startOffset, endOffset)
  }

  override fun replaceText(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: String,
  ) {
    vimPluginApi.replaceText(editor, caretId, startOffset, endOffset, text)
  }

  override fun replaceTextBlockwise(
    caretId: CaretId,
    startOffset: Int,
    endOffset: Int,
    text: List<String>,
  ) {
    vimPluginApi.replaceTextBlockwise(editor, caretId, startOffset, endOffset, text)
  }

  override fun updateCaret(caretId: CaretId, info: CaretInfo) {
    vimPluginApi.updateCaret(editor, caretId, info)
  }

}
internal fun TransactionImpl.executeChange(action: Transaction.() -> Unit) {
  return vimPluginApi.getResourceGuard().change(this, action)
}
