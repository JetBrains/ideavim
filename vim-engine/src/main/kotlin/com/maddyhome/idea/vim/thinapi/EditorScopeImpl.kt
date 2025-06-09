/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.EditorScope
import com.intellij.vim.api.scopes.Read
import com.intellij.vim.api.scopes.Transaction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector

class EditorScopeImpl(
  private val vimEditor: VimEditor,
  private val context: ExecutionContext,
) : EditorScope() {
  override fun <T> ideRead(block: Read.() -> T): T {
    return injector.application.runReadAction {
      val read = ReadImpl(vimEditor, context)
      return@runReadAction block(read)
    }
  }

  override fun ideChange(block: Transaction.() -> Unit) {
    return injector.application.runWriteAction {
      val transaction = TransactionImpl(vimEditor, context)
      transaction.block()
    }
  }
}