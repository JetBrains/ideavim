/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.commandline

import com.intellij.vim.api.scopes.VimPluginDsl
import com.intellij.vim.api.scopes.VimScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Scope for interacting with the Vim command line.
 */
@VimPluginDsl
abstract class CommandLineScope {
  /**
   * Reads input from the command line and processes it with the provided function.
   *
   * @param prompt The prompt to display at the beginning of the command line.
   * @param finishOn The character that, when entered, will finish the input process. If null, only Enter will finish.
   * @param callback A function that will be called with the entered text when input is complete.
   */
  abstract fun input(prompt: String, finishOn: Char? = null, callback: VimScope.(String) -> Unit)

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
   * @param block A suspend function with CommandLineRead receiver that contains the read operations to perform.
   * @return A Deferred that will complete with the result of the block execution.
   */
  @OptIn(ExperimentalContracts::class)
  fun <T> read(block: suspend CommandLineRead.() -> T): Deferred<T> {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
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
   * @param block A suspend function with CommandLineTransaction receiver that contains the write operations to perform.
   * @return A Job that represents the ongoing execution of the block.
   */
  @OptIn(ExperimentalContracts::class)
  fun change(block: suspend CommandLineTransaction.() -> Unit): Job {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ideChange(block)
  }

  protected abstract fun <T> ideRead(block: suspend CommandLineRead.() -> T): Deferred<T>
  protected abstract fun ideChange(block: suspend CommandLineTransaction.() -> Unit): Job
}