/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa

/**
 * Delimits the number of times that a multi should
 * make a certain atom repeat itself
 */
internal sealed class MultiDelimiter {
  /**
   * Represents an integer boundary
   *
   * @param i The boundary of the multi
   */
  data class IntMultiDelimiter(val i: Int) : MultiDelimiter()

  /**
   * Represents an infinite boundary
   */
  object InfiniteMultiDelimiter : MultiDelimiter()
}