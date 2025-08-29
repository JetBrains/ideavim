/*
 * Copyright 2003-2025 The IdeaVim authors
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

class EditorScopeImpl : EditorScope() {
  override fun <T> ideRead(block: ReadScope.() -> T): T {
    return injector.application.runReadAction {
      val readScope = ReadScopeImpl()
      block(readScope)
    }
  }

  override fun ideChange(block: Transaction.() -> Unit) {
    injector.application.invokeAndWait {
      injector.application.runWriteAction {
        val transaction = TransactionImpl()
        transaction.block()
      }
    }
  }
}