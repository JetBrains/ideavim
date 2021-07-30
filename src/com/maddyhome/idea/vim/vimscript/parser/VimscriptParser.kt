/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.vimscript.parser

import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptLexer
import com.maddyhome.idea.vim.vimscript.parser.generated.VimscriptParser
import com.maddyhome.idea.vim.vimscript.parser.visitors.CommandVisitor
import com.maddyhome.idea.vim.vimscript.parser.visitors.ExpressionVisitor
import com.maddyhome.idea.vim.vimscript.parser.visitors.ScriptVisitor
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree

object VimscriptParser {

  fun parse(text: String): Script {
    val parser = getParser(text)
    val AST: ParseTree = parser.script()
    val scriptVisitor = ScriptVisitor
    return scriptVisitor.visit(AST)
  }

  fun parseExpression(text: String): Expression {
    val parser = getParser(text)
    val AST: ParseTree = parser.expr()
    return ExpressionVisitor.visit(AST)
  }

  // todo should return Command after all the oldCommands genocide
  fun parseCommand(text: String): Executable {
    val parser = getParser(text)
    val AST: ParseTree = parser.command()
    return CommandVisitor.visit(AST)
  }

  private fun getParser(text: String): VimscriptParser {
    val input: CharStream = CharStreams.fromString(text)
    val lexer = VimscriptLexer(input)
    val tokens = CommonTokenStream(lexer)
    return VimscriptParser(tokens)
  }
}
