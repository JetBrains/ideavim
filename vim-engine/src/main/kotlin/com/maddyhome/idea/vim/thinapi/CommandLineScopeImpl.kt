/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.CommandLineScope
import com.intellij.vim.api.scopes.VimScope
import com.intellij.vim.api.scopes.commandline.CommandLineRead
import com.intellij.vim.api.scopes.commandline.CommandLineTransaction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner

class CommandLineScopeImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : CommandLineScope() {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  private val vimContext: ExecutionContext
    get() = injector.executionContextManager.getEditorExecutionContext(vimEditor)

  override fun input(prompt: String, finishOn: Char?, callback: VimScope.(String) -> Unit) {
    val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
    injector.commandLine.readInputAndProcess(vimEditor, vimContext, prompt, finishOn) {
      vimScope.callback(it)
    }
  }

  override fun <T> ideRead(block: CommandLineRead.() -> T): T {
    return injector.application.runReadAction {
      val read = CommandLineReadImpl()
      return@runReadAction block(read)
    }
  }

  override fun ideChange(block: CommandLineTransaction.() -> Unit) {
    return injector.application.runWriteAction {
      val transaction = CommandLineTransactionImpl()
      transaction.block()
    }
  }
}