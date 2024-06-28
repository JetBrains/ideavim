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
}