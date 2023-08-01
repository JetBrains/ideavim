/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa

import com.maddyhome.idea.vim.regexp.nfa.matcher.Matcher

/**
 * Represents a single state of a NFA.
 */
internal class NFAState (
  /**
   * Tells whether the state is an accept state or not
   */
  var isAccept: Boolean = false,

  /**
   * All the transitions from this state. Order matters.
   * Transitions with higher priority should be in lower
   * indexes. This is relevant for the implementation of
   * lazy quantifiers.
   */
  var transitions: ArrayList<Pair<Matcher, NFAState>> = ArrayList()
) {

  /**
   * Adds a new transition from the state to another,
   * that can be taken if the given matcher matches.
   *
   * @param dest    The destination state of the new transition
   * @param matcher The matcher used to check if the new transition can be taken
   */
  fun addTransition(dest: NFAState, matcher: Matcher) {
    transitions.add(Pair(matcher, dest))
  }

}