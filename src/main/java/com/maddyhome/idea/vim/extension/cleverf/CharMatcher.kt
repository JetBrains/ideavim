/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

/**
 * Decides whether a character in the buffer matches the searched [char], honouring `g:clever_f_ignore_case`,
 * `g:clever_f_smart_case` and `g:clever_f_chars_match_any_signs`.
 */
internal class CharMatcher(private val char: Char) {
  private val matchesAnySign = char in CleverFSettings.charsMatchAnySigns
  private val ignoreCase = (CleverFSettings.smartCase && char.isLowerCase()) || CleverFSettings.ignoreCase

  fun matches(c: Char): Boolean = if (matchesAnySign) c in SIGNS else c.equals(char, ignoreCase)

  private companion object {
    const val SIGNS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
  }
}
