/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.VimPluginApi
import com.intellij.vim.api.scopes.read.Read
import com.intellij.vim.api.scopes.read.ReadImpl
import com.intellij.vim.api.scopes.read.executeRead
import com.intellij.vim.api.scopes.transaction.Transaction
import com.intellij.vim.api.scopes.transaction.TransactionImpl
import com.intellij.vim.api.scopes.transaction.executeChange
import com.intellij.vim.api.scopes.vim.VimScope
import com.intellij.vim.api.scopes.vim.VimScopeImpl
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


@OptIn(ExperimentalContracts::class)
fun <T> vimScope(
  vimEditor: VimEditor,
  context: ExecutionContext,
  vimApi: VimPluginApi,
  block: VimScope.() -> T,
): T {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  val vimScope = VimScopeImpl(vimEditor, context, vimApi)
  return vimScope.block()
}

@OptIn(ExperimentalContracts::class)
fun <T> VimScope.read(block: Read.() -> T): T {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  require(this !is Transaction && this !is Read) {
    "Cannot open read or transaction scope within read scope"
  }

  val readImpl = with(this as VimScopeImpl) {
    ReadImpl(editor, context, vimPluginApi)
  }
  return readImpl.executeRead(block)
}

@OptIn(ExperimentalContracts::class)
fun VimScope.change(block: Transaction.() -> Unit) {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  require(this !is Transaction && this !is Read) {
    "Cannot open read or transaction scope within transaction scope"
  }

  val transactionImpl = with(this as VimScopeImpl) {
    val readImpl = ReadImpl(editor, context, vimPluginApi)
    TransactionImpl(editor, context, vimPluginApi, readImpl)
  }
  transactionImpl.executeChange(block)
}

fun VimScope.forEachCaret(block: VimScope.(CaretId) -> Unit) {
  read { getAllCaretIds() }.forEach { caretId -> block(caretId) }
}

fun VimScope.forEachCaretSorted(block: VimScope.(CaretId) -> Unit) {
  read { getAllCaretIdsSortedByOffset() }.forEach { caretId -> block(caretId) }
}
