/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Mode
import com.intellij.vim.api.TextSelectionType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@VimPluginDsl
abstract class VimScope {
  abstract var mode: Mode
  abstract fun getSelectionTypeForCurrentMode(): TextSelectionType?
  abstract fun getVariableInt(name: String): Int?
  abstract fun exportOperatorFunction(name: String, function: VimScope.() -> Boolean)
  abstract fun setOperatorFunction(name: String)
  abstract fun normal(command: String)
  // todo: Use mode instead
  abstract fun exitVisualMode()
  abstract fun nmap(from: String, to: String)
  abstract fun vmap(from: String, to: String)
  abstract fun nmap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit)
  abstract fun vmap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit)
  abstract fun nmap(from: String, label: String, isRepeatable: Boolean, action: VimScope.() -> Unit)
  abstract fun vmap(from: String, label: String, isRepeatable: Boolean, action: VimScope.() -> Unit)

  @OptIn(ExperimentalContracts::class)
  fun <T> read(block: Read.() -> T): T {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return this.ideRead(block)
  }

  @OptIn(ExperimentalContracts::class)
  fun change(block: Transaction.() -> Unit) {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ideChange(block)
  }

  fun forEachCaret(block: VimScope.(CaretId) -> Unit) {
    read { getAllCaretIds() }.forEach { caretId -> block(caretId) }
  }

  fun forEachCaretSorted(block: VimScope.(CaretId) -> Unit) {
    read { getAllCaretIdsSortedByOffset() }.forEach { caretId -> block(caretId) }
  }

  protected abstract fun <T> ideRead(block: Read.() -> T): T
  protected abstract fun ideChange(block: Transaction.() -> Unit)
}