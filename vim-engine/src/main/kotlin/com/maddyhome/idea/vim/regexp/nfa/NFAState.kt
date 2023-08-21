/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa

/**
 * Represents a single state of a NFA.
 */
internal class NFAState {
  /**
   * All the transitions from this state. Order matters.
   * Transitions with higher priority should be in lower
   * indexes. This is relevant for the implementation of
   * lazy quantifiers.
   */
  internal val transitions: ArrayList<NFATransition> = ArrayList()

  /**
   * If this is not null, then when simulation reaches this state,
   * it has to check if this assertion is successful to continue.
   */
  internal var assertion: NFAAssertion? = null

  /**
   * Stores the numbers of the capture groups that start
   * being captured on this state
   */
  internal val startCapture: MutableList<Int> = ArrayList()

  /**
   *  Stores the number of the capture groups that stop
   *  being captured on this state
   */
  internal val endCapture: MutableList<Int> = ArrayList()

  /**
   *  Stores the number of the capture groups that stop
   *  being captured on this state, even if that group
   *  had already been set to stop being captured
   */
  internal val forceEndCapture: MutableList<Int> = ArrayList()

  /**
   * Adds a new transition from this state. This transition
   * has the lowest priority so far.
   *
   * @param transition The transition that is to be added
   */
  fun addTransition(transition: NFATransition) {
    transitions.add(transition)
  }
}