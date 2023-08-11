/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser

import com.maddyhome.idea.vim.regexp.parser.generated.RegexParser
import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.TokenStream

internal class RegexParser(input: TokenStream?) : RegexParser(input) {

  /**
   * Override default error handling strategy
   * to bail out at first syntax error
   */
  init {
    errorHandler = BailErrorStrategy()
  }
}