/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi.editor

import com.intellij.vim.api.scopes.editor.EditorScope
import com.intellij.vim.api.scopes.editor.ReadScope
import com.intellij.vim.api.scopes.editor.Transaction
import com.maddyhome.idea.vim.api.injector

class EditorScopeImpl(
  private val projectId: String?,
) : EditorScope() {
  override fun <T> ideRead(block: ReadScope.() -> T): T {
    return injector.application.runReadAction {
      val readScope = ReadScopeImpl(projectId)
      block(readScope)
    }
  }

  override fun ideChange(block: Transaction.() -> Unit) {
    injector.application.invokeAndWait {
      val transaction = TransactionImpl(projectId)
      transaction.block()
    }
  }
}
