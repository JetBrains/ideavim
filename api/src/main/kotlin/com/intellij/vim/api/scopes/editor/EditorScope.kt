/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.editor

import com.intellij.vim.api.scopes.VimApiDsl
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Scope that provides access to editor functions.
 */
@VimApiDsl
abstract class EditorScope {
  /**
   * Executes a read-only operation on the editor.
   *
   * This function provides access to read-only operations through the [Read] interface.
   * It runs the provided block under a read lock to ensure thread safety when accessing editor state.
   * The operation is executed asynchronously and returns a [kotlinx.coroutines.Deferred] that can be awaited for the result.
   *
   * Example usage:
   * ```
   * editor {
   *   val text = read {
   *     text // Access the editor's text content
   *   }.await()
   * }
   * ```
   *
   * @param block A suspending lambda with [Read] receiver that contains the read operations to perform
   * @return A [kotlinx.coroutines.Deferred] that completes with the result of the block execution
   */
  @OptIn(ExperimentalContracts::class)
  fun <T> read(block: suspend ReadScope.() -> T): Deferred<T> {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return this.ideRead(block)
  }

  /**
   * Executes a write operation that modifies the editor's state.
   *
   * This function provides access to write operations through the [Transaction] interface.
   * It runs the provided block under a write lock to ensure thread safety when modifying editor state.
   * The operation is executed asynchronously and returns a [kotlinx.coroutines.Job] that can be joined to wait for completion.
   *
   * Example usage:
   * ```
   * editor {
   *   val job = change {
   *     // Modify editor content
   *     replaceText(startOffset, endOffset, newText)
   *
   *     // Add highlights
   *     val highlightId = addHighlight(startOffset, endOffset, backgroundColor, foregroundColor)
   *   }
   *   job.join() // Wait for the changes to complete
   * }
   * ```
   *
   * @param block A suspending lambda with [Transaction] receiver that contains the write operations to perform
   * @return A [kotlinx.coroutines.Job] that completes when all write operations are finished
   */
  @OptIn(ExperimentalContracts::class)
  fun change(block: suspend Transaction.() -> Unit): Job {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ideChange(block)
  }

  protected abstract fun <T> ideRead(block: suspend ReadScope.() -> T): Deferred<T>
  protected abstract fun ideChange(block: suspend Transaction.() -> Unit): Job
}