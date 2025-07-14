/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@VimPluginDsl
abstract class EditorScope {
  @OptIn(ExperimentalContracts::class)
  fun <T> read(block: suspend Read.() -> T): Deferred<T> {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return this.ideRead(block)
  }

  @OptIn(ExperimentalContracts::class)
  fun change(block: suspend Transaction.() -> Unit): Job {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ideChange(block)
  }

  protected abstract fun <T> ideRead(block: suspend Read.() -> T): Deferred<T>
  protected abstract fun ideChange(block: suspend Transaction.() -> Unit): Job
}