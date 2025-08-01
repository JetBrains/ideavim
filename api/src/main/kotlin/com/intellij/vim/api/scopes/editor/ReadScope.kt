/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.editor

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.scopes.VimApiDsl
import com.intellij.vim.api.scopes.editor.caret.CaretRead

/**
 * Interface that provides functions that open CaretRead scope.
 */
@VimApiDsl
interface ReadScope: Read {
  /**
   * Executes the provided block for each caret in the editor and returns a list of results.
   *
   * This function allows you to perform operations on all carets in the editor in a single call.
   * The block is executed with each caret as the receiver, and the results are collected into a list.
   *
   * Example usage:
   * ```kotlin
   * editor {
   *   val caretOffsets = forEachCaret {
   *     offset // Get the offset of each caret
   *   }
   *   // caretOffsets is a List<Int> containing the offset of each caret
   * }
   * ```
   *
   * @param block The block to execute for each caret
   * @return A list containing the results of executing the block for each caret
   */
  suspend fun <T> forEachCaret(block: suspend CaretRead.() -> T): List<T>

  /**
   * Executes the provided block with a specific caret as the receiver.
   *
   * This function allows you to perform operations on a specific caret identified by its ID.
   *
   * Example usage:
   * ```kotlin
   * editor {
   *   with(caretId) {
   *     // Perform operations on the specific caret
   *     val caretOffset = offset
   *     val caretLine = line
   *   }
   * }
   * ```
   *
   * @param caretId The ID of the caret to use
   * @param block The block to execute with the specified caret as the receiver
   */
  suspend fun <T> with(caretId: CaretId, block: suspend CaretRead.() -> T): T

  /**
   * Executes the provided block with the primary caret as the receiver.
   *
   * This function allows you to perform operations on the primary caret in the editor.
   *
   * Example usage:
   * ```kotlin
   * editor {
   *   withPrimaryCaret {
   *     // Perform operations on the primary caret
   *     val primaryCaretOffset = offset
   *     val primaryCaretLine = line
   *   }
   * }
   * ```
   *
   * @param block The block to execute with the primary caret as the receiver
   */
  suspend fun <T> withPrimaryCaret(block: suspend CaretRead.() -> T): T
}