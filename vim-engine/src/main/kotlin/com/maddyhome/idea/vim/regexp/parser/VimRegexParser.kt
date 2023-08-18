/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser

import com.maddyhome.idea.vim.regexp.parser.error.BailErrorLexer
import com.maddyhome.idea.vim.regexp.parser.error.VimRegexParserErrorStrategy
import com.maddyhome.idea.vim.regexp.parser.error.VimRegexParserException
import com.maddyhome.idea.vim.regexp.parser.generated.RegexLexer
import com.maddyhome.idea.vim.regexp.parser.generated.RegexParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

internal object VimRegexParser {
  fun parse(pattern: String) : VimRegexParserResult {
    return try {
      val regexLexer = BailErrorLexer(CharStreams.fromString(pattern))
      val tokens = CommonTokenStream(regexLexer)
      val parser = RegexParser(tokens)
      parser.errorHandler = VimRegexParserErrorStrategy()
      val tree = parser.pattern()
      VimRegexParserResult.Success(tree, getCaseSensitivitySettings(regexLexer))
    } catch (e: VimRegexParserException) {
      VimRegexParserResult.Failure()
    }
  }

  private fun getCaseSensitivitySettings(lexer: RegexLexer) : CaseSensitivitySettings {
    return when (lexer.ignoreCase) {
      // explicitly compare with true and false, since it might be null
      true -> CaseSensitivitySettings.IGNORE_CASE
      false -> CaseSensitivitySettings.NO_IGNORE_CASE
      else -> CaseSensitivitySettings.DEFAULT
    }
  }
}