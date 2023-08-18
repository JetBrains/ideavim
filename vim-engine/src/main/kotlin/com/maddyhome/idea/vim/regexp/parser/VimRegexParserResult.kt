/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.parser

import org.antlr.v4.runtime.tree.ParseTree

internal sealed class VimRegexParserResult {
  data class Success(val tree: ParseTree, val caseSensitivitySettings: CaseSensitivitySettings) : VimRegexParserResult()
  data class Failure(val message: String = "Invalid pattern") : VimRegexParserResult()
}

internal enum class CaseSensitivitySettings {
  DEFAULT,
  IGNORE_CASE,
  NO_IGNORE_CASE
}