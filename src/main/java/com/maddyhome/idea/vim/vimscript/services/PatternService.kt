/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.maddyhome.idea.vim.api.VimRegexpService
import com.maddyhome.idea.vim.regexp.RegExp
import com.maddyhome.idea.vim.regexp.RegExp.regmmatch_T

object PatternService : VimRegexpService {

  override fun matches(pattern: String, text: String?, ignoreCase: Boolean): Boolean {
    if (text == null) {
      return false
    }

    val regExp = RegExp()
    val regMatch = regmmatch_T()
    regMatch.rmm_ic = ignoreCase

    regMatch.regprog = regExp.vim_regcomp(pattern, 1)
    regMatch.regprog
    if (regMatch.regprog == null) {
      return false
    }

    // todo optimize me senpai :(
    for (i in 0..text.length) {
      if (regExp.vim_string_contains_regexp(regMatch, text.substring(i))) return true
    }
    return false
  }

  override fun getAllMatches(text: String, pattern: String): List<Pair<Int, Int>> {
    val regExp = RegExp()
    val regMatch = regmmatch_T()
    regMatch.regprog = regExp.vim_regcomp(pattern, 1)
    if (regMatch.regprog == null) {
      return emptyList()
    }

    val result = mutableListOf<Pair<Int, Int>>()
    // FIXME I feel pain just looking at it
    for (i in text.indices) {
      if (regExp.vim_string_contains_regexp(regMatch, text.substring(i))) {
        val matchStart = regMatch.startpos[0]?.col ?: continue
        val matchEnd = regMatch.endpos[0]?.col ?: continue
        result.add(Pair(matchStart + i, matchEnd + i))
      }
    }
    return result
  }
}
