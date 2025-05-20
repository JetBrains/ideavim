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
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


@OptIn(ExperimentalContracts::class)
fun vimInitPluginScope(
  vimApi: VimPluginApi,
  block: VimInitPluginScope.() -> Unit,
) {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  val vimInitPluginScope = object : VimInitPluginScope {
    override val vimPluginApi: VimPluginApi
      get() = vimApi
  }
  return vimInitPluginScope.block()
}

@OptIn(ExperimentalContracts::class)
fun <T> vimPluginScope(
  vimEditor: VimEditor,
  context: ExecutionContext,
  vimApi: VimPluginApi,
  block: VimPluginScope.() -> T,
): T {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  val vimPluginScope = object : VimPluginScope {
    override val editor: VimEditor
      get() = vimEditor
    override val context: ExecutionContext
      get() = context
    override val vimPluginApi: VimPluginApi
      get() = vimApi
  }
  return vimPluginScope.block()
}

@OptIn(ExperimentalContracts::class)
fun <T> VimPluginScope.read(block: Read.() -> T): T {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  require(this !is Transaction && this !is Read) {
    "Cannot open read or transaction scope within read scope"
  }

  val vimEditor: VimEditor = editor
  val vimContext: ExecutionContext = context
  val vimApi: VimPluginApi = vimPluginApi
  val read = object : Read {
    override val editor: VimEditor
      get() = vimEditor
    override val context: ExecutionContext
      get() = vimContext
    override val vimPluginApi: VimPluginApi
      get() = vimApi
  }
  return vimPluginApi.getResourceGuard().read(read, block)
}

@OptIn(ExperimentalContracts::class)
fun VimPluginScope.change(block: Transaction.() -> Unit) {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  require(this !is Transaction && this !is Read) {
    "Cannot open read or transaction scope within transaction scope"
  }

  val vimEditor: VimEditor = editor
  val vimContext: ExecutionContext = context
  val vimApi: VimPluginApi = vimPluginApi
  val transaction = object : Transaction {
    override val editor: VimEditor
      get() = vimEditor
    override val context: ExecutionContext
      get() = vimContext
    override val vimPluginApi: VimPluginApi
      get() = vimApi
  }
  vimPluginApi.getResourceGuard().change(transaction, block)
}

fun VimPluginScope.forEachCaret(block: VimPluginScope.(CaretId) -> Unit) {
  read { getAllCaretIds() }.forEach { caretId -> block(caretId) }
}

fun VimPluginScope.forEachCaretSorted(block: VimPluginScope.(CaretId) -> Unit) {
  read { getAllCaretIdsSortedByOffset() }.forEach { caretId -> block(caretId) }
}
