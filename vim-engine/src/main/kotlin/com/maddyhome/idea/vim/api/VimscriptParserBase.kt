/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.parser.generated.VimscriptLexer
import com.maddyhome.idea.vim.parser.generated.VimscriptParser
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.EngineExCommandProvider
import com.maddyhome.idea.vim.vimscript.model.commands.ExCommandProvider
import com.maddyhome.idea.vim.vimscript.model.commands.ExCommandTree
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.parser.DeletionInfo
import com.maddyhome.idea.vim.vimscript.parser.errors.IdeavimErrorListener
import com.maddyhome.idea.vim.vimscript.parser.visitors.CommandVisitor
import com.maddyhome.idea.vim.vimscript.parser.visitors.ExpressionVisitor
import com.maddyhome.idea.vim.vimscript.parser.visitors.ScriptVisitor
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree

abstract class VimscriptParserBase : com.maddyhome.idea.vim.api.VimscriptParser {
  private companion object {
    private const val MAX_NUMBER_OF_TRIES = 5
  }

  private val logger = vimLogger<VimscriptParser>()

  // Thread-local state to support concurrent parsing
  private val threadLocalState = ThreadLocal.withInitial { ParserState() }

  override val linesWithErrors: MutableList<Int>
    get() = threadLocalState.get().linesWithErrors

  private var tries: Int
    get() = threadLocalState.get().tries
    set(value) { threadLocalState.get().tries = value }

  private var deletionInfo: DeletionInfo
    get() = threadLocalState.get().deletionInfo
    set(value) { threadLocalState.get().deletionInfo = value }

  private class ParserState {
    val linesWithErrors: MutableList<Int> = mutableListOf()
    var tries: Int = 0
    var deletionInfo: DeletionInfo = DeletionInfo()
  }
  protected open val commandProviders: List<ExCommandProvider> = listOf(EngineExCommandProvider)
  override val exCommands: ExCommandTree by lazy {
    val commandTree = ExCommandTree()
    commandProviders.forEach { provider -> provider.getCommands().forEach { commandTree.addCommand(it.key, it.value) } }
    commandTree
  }

  override fun parse(script: String): Script {
    val preprocessedText = uncommentIdeaVimIgnore(getTextWithoutErrors(script))
    linesWithErrors.clear()
    val parser = getParser(addNewlineIfMissing(preprocessedText), true)
    val AST: ParseTree = parser.script()
    val script = if (linesWithErrors.isNotEmpty()) {
      if (tries > MAX_NUMBER_OF_TRIES) {
        // I don't think, that it's possible to enter an infinite recursion with any vimrc, but let's have it just in case
        logger.warn("Reached the maximum number of tries to fix a script. Parsing is stopped.")
        resetParser()
        return Script(listOf())
      } else {
        tries += 1
        parse(preprocessedText)
      }
    } else {
      ScriptVisitor.visit(AST)
    }
    script.units.forEach { it.restoreOriginalRange(deletionInfo) }
    resetParser()
    return script
  }

  override fun parseExpression(expression: String): Expression? {
    val parser = getParser(expression, true)
    val AST: ParseTree = parser.expr()
    if (linesWithErrors.isNotEmpty()) {
      resetParser()
      return null
    }
    return ExpressionVisitor.visit(AST)
  }

  override fun parseCommand(command: String): Command? {
    val parser = getParser(addNewlineIfMissing(command), true)
    val AST: ParseTree = parser.command()
    if (linesWithErrors.isNotEmpty()) {
      resetParser()
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

  override fun parseLetCommand(text: String): Command? {
    val parser = getParser(addNewlineIfMissing(text), true)
    val AST: ParseTree = parser.letCommands()
    if (linesWithErrors.isNotEmpty()) {
      resetParser()
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
    val lineNumbersToDelete = linesWithErrors.sortedDescending()
    val lines = splitToLines(text).toMutableList()
    for (lineNumber in lineNumbersToDelete) {
      // this may happen if we have an error somewhere at the end and parser can't find any matching token till EOF (EOF's line number is lines.size)
      if (lines.size <= lineNumber) {
        logger.warn("Parsing error affects lines till EOF")
      } else {
        deletionInfo.registerDeletion(lines[lineNumber - 1].first, lines[lineNumber - 1].second.length)
        lines.removeAt(lineNumber - 1)
      }
    }
    return lines.joinToString(separator = "") { it.second }
  }

  private fun uncommentIdeaVimIgnore(configuration: String): String {
    val ideavimIgnore = "ideavim ignore"
    val ideavimIgnorePattern = Regex("\"( )*ideavim ignore", RegexOption.IGNORE_CASE)
    val result = StringBuilder()

    var startIndex = 0
    val matches = ideavimIgnorePattern.findAll(configuration, startIndex)
    for (match in matches) {
      result.append(configuration.substring(startIndex, match.range.first))
      result.append(ideavimIgnore)
      startIndex = match.range.last + 1
      val delta = match.range.last - match.range.first + 1 - ideavimIgnore.length
      if (delta > 0) {
        deletionInfo.registerDeletion(match.range.first, delta)
      }
    }
    result.append(configuration.substring(startIndex))
    return result.toString()
  }

  // pair for line start offset + line text
  private fun splitToLines(text: String): List<Pair<Int, String>> {
    val result = mutableListOf<Pair<Int, String>>()

    val currentLine = StringBuilder()
    var currentLineStartOffset = 0

    for ((i, char) in text.withIndex()) {
      currentLine.append(char)
      if (char == '\n') {
        result.add(Pair(currentLineStartOffset, currentLine.toString()))
        currentLineStartOffset = i + 1
        currentLine.clear()
      }
    }
    if (currentLine.isNotEmpty()) {
      result.add(Pair(currentLineStartOffset, currentLine.toString()))
    }
    return result
  }

  private fun resetParser() {
    tries = 0
    linesWithErrors.clear()
    deletionInfo.reset()
  }
}