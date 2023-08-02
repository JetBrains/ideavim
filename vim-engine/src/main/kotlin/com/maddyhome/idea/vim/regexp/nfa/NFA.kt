/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa

import com.maddyhome.idea.vim.regexp.nfa.matcher.EpsilonMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.Matcher

/**
 * Represents a non-deterministic finite automaton.
 */
internal class NFA private constructor(
  /**
   * The start state of the NFA
   */
  private var startState: NFAState,
  /**
   * The end state of the NFA
   */
  private var acceptState: NFAState
) {

  /**
   * Concatenates the NFA with another NFA. The new NFA accepts inputs
   * that are accepted by the old NFA followed the other.
   *
   * @param other The NFA to concatenate with
   * @return The new NFA representing the concatenation
   */
  fun concatenate(other: NFA) : NFA {
    /**
     * The acceptState is guaranteed to not have any transitions, so
     * to concatenate the two NFAs together, make the acceptState of
     * the first NFA transition to everything that the startState of
     * the second transitions two, essentially merging the two states
     * together.
     */
    this.acceptState.transitions = other.startState.transitions

    this.acceptState.isAccept = false
    this.acceptState = other.acceptState

    return this
  }

  /**
   * Unifies the NFA with another NFA. The new NFA accepts inputs
   * that are accepted by either the old NFA or the other.
   *
   * @param other The NFA to unify with
   * @return The new NFA representing the union
   */
  fun unify(other: NFA) : NFA {
    val newStart = NFAState(false)
    val newEnd = NFAState(true)

    newStart.addTransition(this.startState, EpsilonMatcher())
    newStart.addTransition(other.startState, EpsilonMatcher())

    this.acceptState.addTransition(newEnd, EpsilonMatcher())
    this.acceptState.isAccept = false
    other.acceptState.addTransition(newEnd, EpsilonMatcher())
    other.acceptState.isAccept = false

    this.startState = newStart
    this.acceptState = newEnd

    return this
  }

  /**
   * Applies Kleene closure to the NFA. The new NFA accepts inputs
   * that are accepted by the old NFA 0 or more times.
   *
   * @return The new NFA representing the closure
   */
  fun closure() : NFA {
    val newStart = NFAState(false)
    val newEnd = NFAState(true)

    newStart.addTransition(newEnd, EpsilonMatcher())
    newStart.addTransition(this.startState, EpsilonMatcher())

    this.acceptState.addTransition(newEnd, EpsilonMatcher())
    this.acceptState.isAccept = false
    this.acceptState.addTransition(this.startState, EpsilonMatcher())

    this.startState = newStart
    this.acceptState = newEnd

    return this
  }

  fun simulate(input: String, stringPointer : Int = 0, currentState: NFAState = startState) : Boolean {
    if (currentState.isAccept) return true
    for (transition in currentState.transitions) {
      val matcher = transition.first
      if (matcher.matches(input, stringPointer)) {
        val newStringPointer = if (matcher.isEpsilon()) stringPointer else stringPointer + 1
        if (simulate(input, newStringPointer, transition.second)) return true
      }
    }
    return false
  }

  companion object {

    /**
     * Creates a new instance of a NFA, that has two states
     * with an epsilon transition from one to the other.
     *
     * start --ε-> end
     *
     * @return The new NFA instance
     */
    fun fromEpsilon() : NFA {
      return fromMatcher(EpsilonMatcher())
    }

    /**
     * Creates a new instance of a NFA, that has two states
     * with a transition from one state to the other
     * defined by a matcher.
     *
     * start --matcher-> end
     *
     * @param matcher The matcher used for the transition
     * @return The new NFA instance
     */
    fun fromMatcher(matcher: Matcher) : NFA {
      val startState = NFAState(false)
      val acceptState = NFAState(true)

      startState.addTransition(acceptState, matcher)
      return NFA(startState, acceptState)
    }
  }
}