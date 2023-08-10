/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

/**
 * Indicates the result of attempting to match with a Matcher
 */
internal sealed class MatcherResult {
  /**
   * Indicates that the Matcher successfully matched.
   *
   * @param consumed The number of characters consumed
   */
  data class Success(val consumed: Int) : MatcherResult()

  /**
   * Indicates that the Matcher doesn't match
   */
  object Failure : MatcherResult()
}