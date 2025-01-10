/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser.error

import com.maddyhome.idea.vim.regexp.VimRegexErrors
import org.antlr.v4.runtime.DefaultErrorStrategy
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Token

internal class VimRegexParserErrorStrategy : DefaultErrorStrategy() {

  override fun recover(recognizer: Parser?, e: RecognitionException) {
    throw VimRegexParserException(VimRegexErrors.E383)
  }

  override fun recoverInline(recognizer: Parser?): Token {
    throw VimRegexParserException(VimRegexErrors.E383)
  }

  override fun sync(recognizer: Parser?) {}
}