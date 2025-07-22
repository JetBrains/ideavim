/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi.editor

import com.intellij.vim.api.scopes.editor.EditorScope
import com.intellij.vim.api.scopes.editor.Read
import com.intellij.vim.api.scopes.editor.Transaction
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class EditorScopeImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : EditorScope() {
  private val coroutineScope = CoroutineScope(Dispatchers.Unconfined )

  override fun <T> ideRead(block: suspend Read.() -> T): Deferred<T> {
    return injector.application.runReadAction {
      val read = ReadImpl(listenerOwner, mappingOwner)
      return@runReadAction coroutineScope.async { block(read) }
    }
  }

  override fun ideChange(block: suspend Transaction.() -> Unit): Job {
    return injector.application.runWriteAction {
      val transaction = TransactionImpl(listenerOwner, mappingOwner)
      coroutineScope.launch { transaction.block() }
    }
  }
}