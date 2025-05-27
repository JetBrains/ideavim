/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.thin.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.vim.api.ResourceGuard
import com.intellij.vim.api.scopes.read.Read
import com.intellij.vim.api.scopes.transaction.Transaction

class IntelliJResourceGuard : ResourceGuard {
  override fun <T> read(read: Read, block: Read.() -> T): T = ApplicationManager.getApplication().runReadAction<T> {
    read.block()
  }

  override fun change(transaction: Transaction, block: Transaction.() -> Unit) {
    ApplicationManager.getApplication().runWriteAction {
      transaction.block()
    }
  }
}