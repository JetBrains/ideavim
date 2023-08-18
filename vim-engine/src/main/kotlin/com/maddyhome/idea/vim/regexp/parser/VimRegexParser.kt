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
import com.maddyhome.idea.vim.regexp.parser.generated.RegexParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree

internal class VimRegexParser(private val pattern: String) {
  var caseSensitivity = CaseSensitivity.DEFAULT
  fun parse() : ParseTree {
    val regexLexer = BailErrorLexer(CharStreams.fromString(pattern))
    val tokens = CommonTokenStream(regexLexer)
    val parser = RegexParser(tokens)
    parser.errorHandler = VimRegexParserErrorStrategy()
    val tree = parser.pattern()

    caseSensitivity = when (regexLexer.ignoreCase) {
      // explicitly compare with true and false, since it might be null
      true -> CaseSensitivity.IGNORE_CASE
      false -> CaseSensitivity.NO_IGNORE_CASE
      else -> CaseSensitivity.DEFAULT
    }

    return tree
  }

  enum class CaseSensitivity {
    DEFAULT,
    IGNORE_CASE,
    NO_IGNORE_CASE
  }
}