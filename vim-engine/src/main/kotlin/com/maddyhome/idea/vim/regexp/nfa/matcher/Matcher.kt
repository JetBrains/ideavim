/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

/**
 * A matcher is used to decide if a transition can be taken,
 * depending on what character is next in the input as well
 * as other information contained in the editor.
 */
internal interface Matcher {
  /**
   * Determines whether the matcher should match.
   *
   * @param input         The input string
   * @param stringPointer The current index in the input string
   */
  fun matches(input : String, stringPointer : Int) : Boolean

  /**
   * Determines whether the matcher should consume
   * the character that is next in the input.
   */
  fun isEpsilon() : Boolean
}