/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.nfa.NFAState
import com.maddyhome.idea.vim.regexp.nfa.MultiDelimiter

/**
 * This matcher matches if a loop has been iterated a certain
 * amount of times.
 *
 * @param n The lowest amount of times that the loop must be iterated
 * @param m The highest amount of times the loop must be iterated
 */
internal class LoopMatcher(val n: MultiDelimiter.IntMultiDelimiter, val m: MultiDelimiter) : Matcher {

  override fun matches(editor: VimEditor, index: Int, state: NFAState): Boolean {
    if (state.i < n.i) return false

    return when (m) {
      is MultiDelimiter.InfiniteMultiDelimiter -> true
      is MultiDelimiter.IntMultiDelimiter -> state.i <= m.i
    }
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}