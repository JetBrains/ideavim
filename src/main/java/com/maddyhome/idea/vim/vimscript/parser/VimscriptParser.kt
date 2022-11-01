/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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

object VimscriptParser : com.maddyhome.idea.vim.api.VimscriptParser {

  private val logger = logger<VimscriptParser>()
  val linesWithErrors = mutableListOf<Int>()
  private const val MAX_NUMBER_OF_TRIES = 5
  private var tries = 0

  override fun parse(script: String): Script {
    val preprocessedText = uncommentIdeaVimIgnore(getTextWithoutErrors(script))
    linesWithErrors.clear()
    val parser = getParser(addNewlineIfMissing(preprocessedText), true)
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

  override fun parseExpression(expression: String): Expression? {
    val parser = getParser(expression, true)
    val AST: ParseTree = parser.expr()
    if (linesWithErrors.isNotEmpty()) {
      linesWithErrors.clear()
      return null
    }
    return ExpressionVisitor.visit(AST)
  }

  override fun parseCommand(command: String): Command? {
    val parser = getParser(addNewlineIfMissing(command), true)
    val AST: ParseTree = parser.command()
    if (linesWithErrors.isNotEmpty()) {
      linesWithErrors.clear()
      return null
    }
    return CommandVisitor.visit(AST)
  }

  // grammar expects that any command or script ends with a newline character
  private fun addNewlineIfMissing(text: String): String {
    if (text.isEmpty()) return "\n"
    return if (text.last() == '\n') {
      text
    } else if (text.last() == '\r') {
      // fix to do not erase the \r (e.g. :normal /search^M)
      text + "\r\n"
    } else {
      text + "\n"
    }
  }

  fun parseLetCommand(text: String): Command? {
    val parser = getParser(addNewlineIfMissing(text), true)
    val AST: ParseTree = parser.letCommands()
    if (linesWithErrors.isNotEmpty()) {
      linesWithErrors.clear()
      return null
    }
    return CommandVisitor.visit(AST)
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

  private fun uncommentIdeaVimIgnore(configuration: String): String {
    return configuration.replace(Regex("\"( )*ideavim ignore", RegexOption.IGNORE_CASE), "ideavim ignore")
  }
}
