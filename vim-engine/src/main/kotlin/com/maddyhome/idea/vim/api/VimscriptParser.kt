/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.ExCommandTree
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression

/**
 * Parser for VimScript code.
 *
 * This interface is implemented as a singleton and is thread-safe. All parsing operations
 * can be called concurrently from multiple threads without external synchronization.
 */
interface VimscriptParser {
  /**
   * List of line numbers (1-indexed) that had parsing errors during the most recent parse operation
   * on the current thread. This list is cleared at the start of each parse operation.
   *
   * Note: This property is thread-local, meaning each thread has its own independent error list.
   */
  val linesWithErrors: MutableList<Int>

  /**
   * Tree of all available ex commands.
   */
  val exCommands: ExCommandTree

  /**
   * Parses a complete VimScript script.
   *
   * @param script the VimScript code to parse
   * @return the parsed script AST, or an empty Script if parsing fails after multiple retries
   */
  fun parse(script: String): Script

  /**
   * Parses a `:let` command.
   *
   * @param text the let command text to parse
   * @return the parsed command, or null if parsing fails
   */
  fun parseLetCommand(text: String): Command?

  /**
   * Parses a single ex command.
   *
   * @param command the command text to parse
   * @return the parsed command, or null if parsing fails
   */
  fun parseCommand(command: String): Command?

  /**
   * Parses a VimScript expression.
   *
   * @param expression the expression text to parse
   * @return the parsed expression, or null if parsing fails
   */
  fun parseExpression(expression: String): Expression?
}
