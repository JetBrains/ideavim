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

import com.intellij.openapi.diagnostic.logger
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

  private val logger = logger<VimscriptParser>()
  val linesWithErrors = mutableListOf<Int>()
  private const val MAX_NUMBER_OF_TRIES = 5
  var tries = 0

  fun parse(text: String): Script {
    val preprocessedText = getTextWithoutErrors(text)
    linesWithErrors.clear()
    val parser = getParser(preprocessedText + "\n", true) // grammar expects that any script ends with a newline character
    val AST: ParseTree = parser.script()
    return if (linesWithErrors.isNotEmpty()) {
      if (tries > MAX_NUMBER_OF_TRIES) {
        // I don't think, that it's possible to enter an infinite recursion with any vimrc, but let's have it just in case
        logger.warn("Reached the maximum number of tries to fix a script. Parsing is stopped.")
        linesWithErrors.clear()
        tries = 0
        return Script(listOf())
      } else {
        tries += 1
        parse(preprocessedText)
      }
    } else {
      tries = 0
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
    val lines = text.split("\n", "\r\n").toMutableList()
    for (lineNumber in lineNumbersToDelete) {
      // this may happen if we have an error somewhere at the end and parser can't find any matching token till EOF (EOF's line number is lines.size)
      if (lines.size <= lineNumber) {
        logger.warn("Parsing error affects lines till EOF")
      } else {
        lines.removeAt(lineNumber - 1)
      }
    }
    return lines.joinToString(separator = "\n")
  }
}
