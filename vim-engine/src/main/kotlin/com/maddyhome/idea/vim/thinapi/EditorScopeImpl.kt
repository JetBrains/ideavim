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
import com.maddyhome.idea.vim.api.injector

class EditorScopeImpl() : EditorScope() {
  override fun <T> ideRead(block: Read.() -> T): T {
    return injector.application.runReadAction {
      val read = ReadImpl()
      return@runReadAction block(read)
    }
  }

  override fun ideChange(block: Transaction.() -> Unit) {
    return injector.application.runWriteAction {
      val transaction = TransactionImpl()
      transaction.block()
    }
  }
}