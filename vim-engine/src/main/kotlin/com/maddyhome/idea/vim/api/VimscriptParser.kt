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

interface VimscriptParser {
  val linesWithErrors: MutableList<Int>

  /**
   * Human-readable syntax errors collected while parsing the *current* pass. Populated by the error listener and
   * cleared at the start of every parse pass (including the internal error-recovery retries), so it is scratch state.
   * Consumers that want to report parse errors should read [lastParseErrors] instead.
   */
  val errorMessages: MutableList<String>

  /**
   * Human-readable syntax errors from the last top-level [parse] call. Unlike [errorMessages], this survives the
   * internal error-recovery retries and the parser reset, so it can be read after [parse] returns to report the
   * errors to the user (e.g. when executing .ideavimrc).
   */
  val lastParseErrors: List<String>

  val exCommands: ExCommandTree

  fun parse(script: String): Script
  fun parseLetCommand(text: String): Command?
  fun parseCommand(command: String): Command?
  fun parseExpression(expression: String): Expression?
}
