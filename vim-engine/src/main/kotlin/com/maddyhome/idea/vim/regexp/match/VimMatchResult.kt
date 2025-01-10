/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.match

import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.regexp.VimRegexErrors

/**
 * The result of matching a pattern against an editor
 */
sealed class VimMatchResult {

  /**
   * Successful match
   *
   * @param range  The range of indices in the editor text of where the match was found
   * @param value  The string value of the match found
   * @param groups The results of sub-matches corresponding to capture groups
   */
  data class Success(
    val range: TextRange,
    val value: String,
    val groups: VimMatchGroupCollection,
  ) : VimMatchResult()

  /**
   * Match was unsuccessful or not found
   *
   * @param errorCode Code of the error that caused matching to fail
   */
  data class Failure(
    val errorCode: VimRegexErrors,
  ) : VimMatchResult()
}