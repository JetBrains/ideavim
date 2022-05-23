/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
}
