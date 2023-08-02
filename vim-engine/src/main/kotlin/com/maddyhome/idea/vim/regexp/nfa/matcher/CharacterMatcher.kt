/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

/**
 * Matcher used to match against single characters
 */
internal class CharacterMatcher(val char: Char) : Matcher {
  override fun matches(input: String, stringPointer: Int): Boolean {
    return stringPointer < input.length && input[stringPointer] == char
  }

  override fun isEpsilon(): Boolean {
    return false
  }

}