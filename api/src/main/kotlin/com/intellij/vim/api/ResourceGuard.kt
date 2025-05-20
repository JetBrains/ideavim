/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

import com.intellij.vim.api.scopes.Read
import com.intellij.vim.api.scopes.Transaction

interface ResourceGuard {
  fun <T> read(read: Read, block: Read.() -> T): T
  fun change(transaction: Transaction, block: Transaction.() -> Unit)
}