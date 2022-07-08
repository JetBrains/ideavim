package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression

interface VimscriptParser {

  fun parse(script: String): Script
  fun parseCommand(command: String): Command?
  fun parseExpression(expression: String): Expression?
}
