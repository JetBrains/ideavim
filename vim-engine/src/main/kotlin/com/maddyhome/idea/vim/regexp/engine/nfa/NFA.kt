/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.engine.nfa

import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.EpsilonMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.Matcher

/**
 * Represents a non-deterministic finite automaton.
 */
internal class NFA private constructor(
  /**
   * The start state of the NFA
   */
  internal var startState: NFAState,
  /**
   * The end state of the NFA
   */
  internal var acceptState: NFAState,
) {

  /**
   * Concatenates the NFA with another NFA. The new NFA accepts inputs
   * that are accepted by the old NFA followed by the other.
   *
   * @param other The NFA to concatenate with
   *
   * @return The new NFA representing the concatenation
   */
  internal fun concatenate(other: NFA): NFA {
    this.acceptState.addTransition(
      NFATransition(
        EpsilonMatcher(),
        other.startState
      )
    )

    this.acceptState = other.acceptState

    return this
  }

  /**
   * Unifies the NFA with another NFA. The new NFA accepts inputs
   * that are accepted by either the old NFA or the other.
   *
   * @param other The NFA to unify with
   *
   * @return The new NFA representing the union
   */
  internal fun unify(other: NFA): NFA {
    val newStart = NFAState()
    val newEnd = NFAState()

    newStart.addTransition(NFATransition(EpsilonMatcher(), this.startState))
    newStart.addTransition(NFATransition(EpsilonMatcher(), other.startState))

    this.acceptState.addTransition(NFATransition(EpsilonMatcher(), newEnd))
    other.acceptState.addTransition(NFATransition(EpsilonMatcher(), newEnd))

    this.startState = newStart
    this.acceptState = newEnd

    return this
  }

  /**
   * Kleene's closure of the NFA. Allows the NFA to "loop" any amount of times.
   *
   * @param isGreedy Whether the NFA should give priority to consuming as much input as possible
   *
   * @return The new NFA representing the closure
   */
  internal fun closure(isGreedy: Boolean): NFA {
    val newStart = NFAState()
    val newEnd = NFAState()

    if (isGreedy) {
      newStart.addTransition(NFATransition(EpsilonMatcher(), startState))
      newStart.addTransition(NFATransition(EpsilonMatcher(), newEnd))

      acceptState.addTransition(NFATransition(EpsilonMatcher(), startState))
      acceptState.addTransition(NFATransition(EpsilonMatcher(), newEnd))
    } else {
      newStart.addTransition(NFATransition(EpsilonMatcher(), newEnd))
      newStart.addTransition(NFATransition(EpsilonMatcher(), startState))

      acceptState.addTransition(NFATransition(EpsilonMatcher(), newEnd))
      acceptState.addTransition(NFATransition(EpsilonMatcher(), startState))
    }

    startState = newStart
    acceptState = newEnd

    return this
  }

  /**
   * Gives the NFA the choice to jump directly from its start to
   * accept state, without taking any of the inner transitions.
   *
   * @param isGreedy Whether the NFA should give priority to consuming as much input as possible
   *
   * @return The new NFA, that can be matched optionally
   */
  internal fun optional(isGreedy: Boolean): NFA {
    val newStart = NFAState()
    val newEnd = NFAState()

    if (isGreedy) {
      newStart.addTransition(NFATransition(EpsilonMatcher(), startState))
      newStart.addTransition(NFATransition(EpsilonMatcher(), newEnd))
    } else {
      newStart.addTransition(NFATransition(EpsilonMatcher(), newEnd))
      newStart.addTransition(NFATransition(EpsilonMatcher(), startState))
    }

    acceptState.addTransition(NFATransition(EpsilonMatcher(), newEnd))
    startState = newStart
    acceptState = newEnd

    return this
  }

  /**
   * Marks the start and accept states of the NFA to start
   * and end, respectfully, the capturing of a group.
   *
   * @param groupNumber The number of the capture group
   * @param force       Whether the state should force-end the capturing of the group
   */
  internal fun capture(groupNumber: Int, force: Boolean = true) {
    this.startState.startCapture.add(groupNumber)
    if (force) this.acceptState.forceEndCapture.add(groupNumber)
    else this.acceptState.endCapture.add(groupNumber)
  }

  /**
   * Marks the NFA to be asserted during simulation. The simulation
   * may or may not consume input, and can be positive (simulation must
   * succeed) or negative (simulation must fail).
   *
   * @param shouldConsume Whether the assertion should consume input.
   * @param isPositive    Whether the assertion is positive or negative.
   *
   * @return The NFA instance marked for assertion.
   */
  internal fun assert(shouldConsume: Boolean, isPositive: Boolean, isAhead: Boolean, limit: Int = 0): NFA {
    val newStart = NFAState()
    val newEnd = NFAState()

    newStart.assertion = NFAAssertion(
      shouldConsume,
      isPositive,
      isAhead,
      startState,
      acceptState,
      newEnd,
      limit
    )

    acceptState = newEnd
    startState = newStart

    return this
  }

  /**
   * Sets the start state of the NFA to mark where the whole match should begin.
   */
  internal fun startMatch() {
    this.startState.startCapture.add(0)
  }

  /**
   * Sets the accept state of the NFA to mark where the whole match should end.
   */
  internal fun endMatch() {
    this.acceptState.forceEndCapture.add(0)
  }


  internal companion object {

    /**
     * Creates a new instance of a NFA, that has a single
     * state.
     *
     * @return The new NFA instance
     */
    internal fun fromSingleState(): NFA {
      val state = NFAState()
      return NFA(state, state)
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
    internal fun fromMatcher(matcher: Matcher): NFA {
      val startState = NFAState()
      val acceptState = NFAState()

      startState.addTransition(NFATransition(matcher, acceptState))
      return NFA(startState, acceptState)
    }
  }
}
