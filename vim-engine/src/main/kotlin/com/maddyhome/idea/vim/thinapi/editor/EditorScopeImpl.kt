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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class EditorScopeImpl : EditorScope() {
  private val coroutineScope = CoroutineScope(Dispatchers.Unconfined )

  override fun <T> ideRead(block: suspend ReadScope.() -> T): Deferred<T> {
    return injector.application.runReadAction {
      val readScope = ReadScopeImpl()
      return@runReadAction coroutineScope.async { block(readScope) }
    }
  }

  override fun ideChange(block: suspend Transaction.() -> Unit): Job {
    return injector.application.runWriteAction {
      val transaction = TransactionImpl()
      coroutineScope.launch { transaction.block() }
    }
  }
}