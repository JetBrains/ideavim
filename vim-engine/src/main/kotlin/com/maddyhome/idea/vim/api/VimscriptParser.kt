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

public interface VimscriptParser {
  public val exCommands: ExCommandTree

  public fun parse(script: String): Script
  public fun parseLetCommand(text: String): Command?
  public fun parseCommand(command: String): Command?
  public fun parseExpression(expression: String): Expression?
}
