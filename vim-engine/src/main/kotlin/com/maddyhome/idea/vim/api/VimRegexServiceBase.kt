/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.regexp.VimRegex
import com.maddyhome.idea.vim.regexp.VimRegexOptions
import com.maddyhome.idea.vim.regexp.match.VimMatchResult

class VimRegexServiceBase : VimRegexpService {
  override fun matches(pattern: String, text: String?, ignoreCase: Boolean): Boolean {
    if (text == null) {
      return false
    }
    val options = if (ignoreCase) enumSetOf(VimRegexOptions.IGNORE_CASE) else noneOfEnum()
    return VimRegex(pattern).containsMatchIn(text, options)
  }

  override fun getAllMatches(text: String, pattern: String): List<Pair<Int, Int>> {
    val matches = VimRegex(pattern).findAll(text)
    return matches.map { it.range.startOffset to it.range.endOffset }
  }

  override fun findNext(
    pattern: String,
    text: String,
    start: Int,
    includeStartPosition: Boolean
  ): Pair<Int, Int>? {
    val options = if (includeStartPosition) enumSetOf(VimRegexOptions.CAN_MATCH_START_LOCATION) else noneOfEnum()
    return VimRegex(pattern).findNext(text, start, options).let { it as? VimMatchResult.Success }
      ?.let { match ->
        val range = match.range
        range.startOffset to range.endOffset
      }
  }

  override fun findPrevious(
    pattern: String,
    text: String,
    start: Int,
  ): Pair<Int, Int>? {
    return VimRegex(pattern).findPrevious(text, start).let { it as? VimMatchResult.Success }
      ?.let { match ->
        val range = match.range
        range.startOffset to range.endOffset
      }
  }
}