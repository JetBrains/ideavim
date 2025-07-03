/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.scopes.commandline.CommandLineRead
import com.intellij.vim.api.scopes.commandline.CommandLineTransaction
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Interface for interacting with the Vim command line.
 * 
 * The command line is used for entering Ex commands, search patterns, and other input.
 * This scope provides methods to create, manipulate, and interact with the command line.
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

  @OptIn(ExperimentalContracts::class)
  fun <T> read(block: CommandLineRead.() -> T): T {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return this.ideRead(block)
  }

  @OptIn(ExperimentalContracts::class)
  fun change(block: CommandLineTransaction.() -> Unit) {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ideChange(block)
  }

  protected abstract fun <T> ideRead(block: CommandLineRead.() -> T): T
  protected abstract fun ideChange(block: CommandLineTransaction.() -> Unit)
}