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

import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.parser.errors.IdeavimErrorListener
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

  val linesWithErrors = mutableListOf<Int>()

  fun parse(text: String): Script {
    val preprocessedText = getTextWithoutErrors(text)
    linesWithErrors.clear()
    val parser = getParser(preprocessedText + "\n", true) // grammar expects that any script ends with a newline character
    val AST: ParseTree = parser.script()
    return if (linesWithErrors.isNotEmpty()) {
      parse(preprocessedText)
    } else {
      ScriptVisitor.visit(AST)
    }
  }

  fun parseExpression(text: String): Expression {
    val parser = getParser(text)
    val AST: ParseTree = parser.expr()
    val expression = ExpressionVisitor.visit(AST)
    return expression
  }

  fun parseCommand(text: String): Command {
    val parser = getParser(text + "\n") // grammar expects that any command ends with a newline character
    val AST: ParseTree = parser.command()
    val command = CommandVisitor.visit(AST)
    return command
  }

  private fun getParser(text: String, addListener: Boolean = false): VimscriptParser {
    val input: CharStream = CharStreams.fromString(text)
    val lexer = VimscriptLexer(input)
    val tokens = CommonTokenStream(lexer)
    val parser = VimscriptParser(tokens)
    parser.errorListeners.clear()
    if (addListener) {
      parser.addErrorListener(IdeavimErrorListener())
    }
    return parser
  }

  private fun getTextWithoutErrors(text: String): String {
    linesWithErrors.sortDescending()
    val lineNumbersToDelete = linesWithErrors
    val lines = text.split("\n").toMutableList()
    for (lineNumber in lineNumbersToDelete) {
      lines.removeAt(lineNumber - 1)
    }
    return lines.joinToString(separator = "\n")
  }
}
