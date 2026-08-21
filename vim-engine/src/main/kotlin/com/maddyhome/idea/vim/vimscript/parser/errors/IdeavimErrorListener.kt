/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.parser.errors

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.misc.Interval

class IdeavimErrorListener : BaseErrorListener() {
  private val logger = vimLogger<IdeavimErrorListener>()

  companion object {
    val testLogger: MutableList<String> = mutableListOf<String>()

    private val statementStartRules = setOf("script", "blockMember")

    private val expectedTokensRegex = Regex(" expecting \\{.*}")
  }

  override fun syntaxError(
    recognizer: Recognizer<*, *>?,
    offendingSymbol: Any?,
    line: Int,
    charPositionInLine: Int,
    msg: String?,
    e: RecognitionException?,
  ) {
    injector.vimscriptParser.linesWithErrors.add(line)
    val diagnostic = "line $line:$charPositionInLine $msg"
    injector.vimscriptParser.errorMessages.add(
      getUserFacingMessage(recognizer, offendingSymbol, line, charPositionInLine, msg)
    )
    if (injector.application.isUnitTest()) {
      testLogger.add(diagnostic)
    } else {
      logger.warn(diagnostic)
    }
  }

  private fun getUserFacingMessage(
    recognizer: Recognizer<*, *>?,
    offendingSymbol: Any?,
    line: Int,
    charPositionInLine: Int,
    msg: String?,
  ): String {
    val unknownCommand = getUnknownCommandText(recognizer, offendingSymbol)
    if (unknownCommand != null) {
      return injector.messages.message("E492", unknownCommand)
    }
    return "line $line:$charPositionInLine ${msg?.replace(expectedTokensRegex, "")}"
  }

  private fun getUnknownCommandText(recognizer: Recognizer<*, *>?, offendingSymbol: Any?): String? {
    val parser = recognizer as? Parser ?: return null
    val ruleIndex = parser.ruleContext?.ruleIndex ?: return null
    if (parser.ruleNames.getOrNull(ruleIndex) !in statementStartRules) return null

    val token = offendingSymbol as? Token ?: return null
    val input = token.inputStream ?: return null
    // The EOF token starts past the end of the input, and there's no command text to report
    if (token.startIndex < 0 || token.startIndex >= input.size()) return null

    val restOfInput = input.getText(Interval.of(token.startIndex, input.size() - 1))
    return restOfInput.substringBefore('\n').trimEnd().ifEmpty { null }
  }
}
