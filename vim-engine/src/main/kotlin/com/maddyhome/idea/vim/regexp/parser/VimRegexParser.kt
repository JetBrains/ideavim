/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser

import com.maddyhome.idea.vim.parser.generated.RegexLexer
import com.maddyhome.idea.vim.parser.generated.RegexParser
import com.maddyhome.idea.vim.regexp.parser.error.BailErrorLexer
import com.maddyhome.idea.vim.regexp.parser.error.VimRegexParserErrorStrategy
import com.maddyhome.idea.vim.regexp.parser.error.VimRegexParserException
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

/**
 * Represents a parser of Vim's patterns.
 * This is a singleton.
 */
internal object VimRegexParser {

  /**
   * Tries to parse a given pattern
   *
   * @param pattern The Vim pattern that is to be parsed
   *
   * @return The result, either successful or not, of trying to parse the pattern
   */
  fun parse(pattern: String) : VimRegexParserResult {
    return try {
      val regexLexer = BailErrorLexer(CharStreams.fromString(pattern))
      val tokens = CommonTokenStream(regexLexer)
      val parser = RegexParser(tokens)
      parser.errorHandler = VimRegexParserErrorStrategy()
      parser.errorListeners.clear()
      val tree = parser.pattern()
      VimRegexParserResult.Success(tree, getCaseSensitivitySettings(regexLexer))
    } catch (e: VimRegexParserException) {
      VimRegexParserResult.Failure(e.errorCode)
    }
  }

  /**
   * Auxiliary function used to get the case sensitivity settings from the lexer.
   * The lexer has an internal flag, ignoreCase, that is initially null; if it
   * then comes across a \c, it sets this flag to true, and if it comes across a
   * \C, sets it to false.
   */
  private fun getCaseSensitivitySettings(lexer: RegexLexer) : CaseSensitivitySettings {
    return when (lexer.ignoreCase) {
      // explicitly compare with true and false, since it might be null
      true -> CaseSensitivitySettings.IGNORE_CASE
      false -> CaseSensitivitySettings.NO_IGNORE_CASE
      else -> CaseSensitivitySettings.DEFAULT
    }
  }
}