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
  var transitions: ArrayList<NFATransition> = ArrayList(),

  var i: Int = 0,

  val startCapture: MutableList<Int> = ArrayList(),
  val endCapture: MutableList<Int> = ArrayList()
) {

  /**
   * Adds a new transition from this state
   *
   * @param transition The transition that is to be added
   */
  fun addTransition(transition: NFATransition) {
    transitions.add(transition)
  }

}