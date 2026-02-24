/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.editor

import com.intellij.vim.api.scopes.VimApiDsl
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
   * This function provides access to read-only operations through the [ReadScope] interface.
   * It runs the provided block under a read lock to ensure thread safety when accessing editor state.
   *
   * Example usage:
   * ```
   * editor {
   *   val text = read {
   *     text // Access the editor's text content
   *   }
   * }
   * ```
   *
   * @param block A lambda with [ReadScope] receiver that contains the read operations to perform
   * @return The result of the block execution
   */
  @OptIn(ExperimentalContracts::class)
  fun <T> read(block: ReadScope.() -> T): T {
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
   *
   * Example usage:
   * ```
   * editor {
   *   change {
   *     // Modify editor content
   *     replaceText(startOffset, endOffset, newText)
   *
   *     // Add highlights
   *     val highlightId = addHighlight(startOffset, endOffset, backgroundColor, foregroundColor)
   *   }
   * }
   * ```
   *
   * @param block A lambda with [Transaction] receiver that contains the write operations to perform
   */
  @OptIn(ExperimentalContracts::class)
  fun change(block: Transaction.() -> Unit) {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ideChange(block)
  }

  protected abstract fun <T> ideRead(block: ReadScope.() -> T): T
  protected abstract fun ideChange(block: Transaction.() -> Unit)
}