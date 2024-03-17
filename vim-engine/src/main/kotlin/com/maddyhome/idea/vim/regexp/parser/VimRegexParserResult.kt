/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser

import com.maddyhome.idea.vim.regexp.VimRegexErrors
import org.antlr.v4.runtime.tree.ParseTree

/**
 * The result of trying to parse a string representing a Vim
 * regular expression into a parse tree
 */
internal sealed class VimRegexParserResult {

  /**
   * Represents a successful parse
   *
   * @param tree                    The parse tree of the parsed regular expression
   * @param caseSensitivitySettings The value of the case sensitivity flag in the regular expression
   */
  data class Success(val tree: ParseTree, val caseSensitivitySettings: CaseSensitivitySettings) : VimRegexParserResult()

  /**
   * Represents an unsuccessful parse
   *
   * @param errorCode A message giving output on why parsing failed
   */
  data class Failure(val errorCode: VimRegexErrors) : VimRegexParserResult()
}

/**
 * Represents the case sensitivity setting of a regular expression
 * IGNORE_CASE is for \c, NO_IGNORE_CASE for \C, and DEFAULT when
 * none of these tokens are present.
 *
 * @see :help /ignorecase
 */
internal enum class CaseSensitivitySettings {
  DEFAULT,
  IGNORE_CASE,
  NO_IGNORE_CASE
}