/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi.commandline

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.scopes.commandline.CommandLineRead
import com.intellij.vim.api.scopes.commandline.CommandLineScope
import com.intellij.vim.api.scopes.commandline.CommandLineTransaction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.thinapi.VimApiImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class CommandLineScopeImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : CommandLineScope() {
  private val coroutineScope = CoroutineScope(Dispatchers.Unconfined )

  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  private val vimContext: ExecutionContext
    get() = injector.executionContextManager.getEditorExecutionContext(vimEditor)

  override fun input(prompt: String, finishOn: Char?, callback: VimApi.(String) -> Unit) {
    val vimApi = VimApiImpl(listenerOwner, mappingOwner)
    injector.commandLine.readInputAndProcess(vimEditor, vimContext, prompt, finishOn) {
      vimApi.callback(it)
    }
  }

  override fun <T> ideRead(block: suspend CommandLineRead.() -> T): Deferred<T> {
    return injector.application.runReadAction {
      val read = CommandLineReadImpl()
      return@runReadAction coroutineScope.async { block(read) }
    }
  }

  override fun ideChange(block: suspend CommandLineTransaction.() -> Unit): Job {
    return injector.application.runWriteAction {
      val transaction = CommandLineTransactionImpl()
      coroutineScope.launch { transaction.block() }
    }
  }
}