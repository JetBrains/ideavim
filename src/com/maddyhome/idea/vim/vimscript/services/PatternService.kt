package com.maddyhome.idea.vim.vimscript.services

import com.maddyhome.idea.vim.regexp.RegExp
import com.maddyhome.idea.vim.regexp.RegExp.regmmatch_T

object PatternService {

  fun matches(pattern: String, text: String?, ignoreCase: Boolean = false): Boolean {
    if (text == null) {
      return false
    }

    val regExp = RegExp()
    val regMatch = regmmatch_T()
    regMatch.rmm_ic = ignoreCase

    regMatch.regprog = regExp.vim_regcomp(pattern, 1)
    if (regMatch.regprog == null) {
      return false
    }

    return regExp.vim_string_contains_regexp(regMatch, text)
  }
}
