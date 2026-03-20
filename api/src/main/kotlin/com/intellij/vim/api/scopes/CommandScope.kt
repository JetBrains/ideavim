/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.VimApi

/**
 * Scope that provides access to command registration and operator functions.
 *
 * Example usage:
 * ```kotlin
 * // Lambda style
 * api.commands {
 *     register("MyCommand") { cmd, startLine, endLine ->
 *         println("Command executed: $cmd on lines $startLine-$endLine")
 *     }
 * }
 *
 * // Direct object style
 * api.commands().register("MyCommand") { cmd, startLine, endLine ->
 *     println("Command executed: $cmd")
 * }
 * ```
 */
@VimApiDsl
interface CommandScope {
  /**
   * Registers a new Vim command.
   *
   * Example usage:
   * ```
   * register("MyCommand") { cmd, startLine, endLine ->
   *     println("Command executed: $cmd on lines $startLine-$endLine")
   * }
   * ```
   *
   * @param command The name of the command to register, as entered by the user.
   * @param block The logic to execute when the command is invoked. Receives the command name
   *              entered by the user, and the 0-based start and end line numbers of the
   *              ex-command range (e.g., from `:1,3MyCommand` or `:g/pattern/MyCommand`).
   */
  fun register(command: String, block: suspend VimApi.(commandText: String, startLine: Int, endLine: Int) -> Unit)

  /**
   * Exports a function that can be used as an operator function in Vim.
   *
   * In Vim, operator functions are used with the `g@` operator to create custom operators.
   *
   * @param name The name to register the function under
   * @param function The function to execute when the operator is invoked
   */
  fun exportOperatorFunction(name: String, function: suspend VimApi.() -> Boolean)

  /**
   * Sets the current operator function to use with the `g@` operator.
   *
   * In Vim, this is equivalent to setting the 'operatorfunc' option.
   *
   * @param name The name of the previously exported operator function
   */
  suspend fun setOperatorFunction(name: String)
}
