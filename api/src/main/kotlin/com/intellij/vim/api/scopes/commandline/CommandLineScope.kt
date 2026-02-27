/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.commandline

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.scopes.VimApiDsl

/**
 * Scope for interacting with the Vim command line.
 */
@VimApiDsl
abstract class CommandLineScope {
  /**
   * Reads input from the command line and processes it with the provided function.
   *
   * @param prompt The prompt to display at the beginning of the command line.
   * @param finishOn The character that, when entered, will finish the input process. If null, only Enter will finish.
   * @param callback A function that will be called with the entered text when input is complete.
   */
  abstract fun input(prompt: String, finishOn: Char? = null, callback: suspend VimApi.(String) -> Unit)

  /**
   * Executes operations on the command line that require a read lock.
   *
   * Example usage:
   * ```kotlin
   * commandLine {
   *  read {
   *    text
   *  }
   * }
   * ```
   *
   * @param block A function with CommandLineRead receiver that contains the read operations to perform.
   *              The block is non-suspend because it runs inside a read lock.
   * @return The result of the block execution.
   */
  suspend fun <T> read(block: CommandLineRead.() -> T): T {
    return this.ideRead(block)
  }

  /**
   * Executes operations that require write lock on the command line.
   *
   * Example usage:
   * ```kotlin
   * // Set command line text
   * commandLineScope {
   *  change {
   *    setText("Hello")
   *   }
   * }
   * ```
   *
   * @param block A function with CommandLineTransaction receiver that contains the write operations to perform.
   *              The block is non-suspend because it runs inside a write lock.
   */
  suspend fun change(block: CommandLineTransaction.() -> Unit) {
    ideChange(block)
  }

  protected abstract fun <T> ideRead(block: CommandLineRead.() -> T): T
  protected abstract fun ideChange(block: CommandLineTransaction.() -> Unit)
}