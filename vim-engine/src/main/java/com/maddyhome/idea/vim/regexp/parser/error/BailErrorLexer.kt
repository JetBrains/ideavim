/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser.error

import com.maddyhome.idea.vim.regexp.parser.generated.RegexLexer
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.LexerNoViableAltException

public class BailErrorLexer(input: CharStream?) : RegexLexer(input) {
  override fun recover(e: LexerNoViableAltException?) {
    throw RuntimeException(e)
  }
}