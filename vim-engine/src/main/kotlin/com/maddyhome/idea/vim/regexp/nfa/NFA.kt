/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.regexp.nfa.matcher.EpsilonMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.Matcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.MatcherResult

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
   * Memory used to store capture groups
   */
  private val groups: VimMatchGroupCollection = VimMatchGroupCollection()

  /**
   * Concatenates the NFA with another NFA. The new NFA accepts inputs
   * that are accepted by the old NFA followed the other.
   *
   * @param other The NFA to concatenate with
   *
   * @return The new NFA representing the concatenation
   */
  internal fun concatenate(other: NFA) : NFA {
    this.acceptState.addTransition(NFATransition(EpsilonMatcher(), other.startState))

    this.acceptState.isAccept = false
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
  internal fun unify(other: NFA) : NFA {
    val newStart = NFAState(false)
    val newEnd = NFAState(true)

    newStart.addTransition(NFATransition(EpsilonMatcher(), this.startState))
    newStart.addTransition(NFATransition(EpsilonMatcher(), other.startState))

    this.acceptState.addTransition(NFATransition(EpsilonMatcher(), newEnd))
    this.acceptState.isAccept = false
    other.acceptState.addTransition(NFATransition(EpsilonMatcher(), newEnd))
    other.acceptState.isAccept = false

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
  internal fun closure(isGreedy: Boolean) : NFA {
    val newStart = NFAState(false)
    val newEnd = NFAState(true)

    if (isGreedy){
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

    acceptState.isAccept = false
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
  internal fun optional(isGreedy: Boolean) {
    val newStart = NFAState(false)
    val newEnd = NFAState(true)

    if (isGreedy) {
      newStart.addTransition(NFATransition(EpsilonMatcher(), startState))
      newStart.addTransition(NFATransition(EpsilonMatcher(), newEnd))
    }
    else {
      newStart.addTransition(NFATransition(EpsilonMatcher(), newEnd))
      newStart.addTransition(NFATransition(EpsilonMatcher(), startState))
    }

    acceptState.addTransition(NFATransition(EpsilonMatcher(), newEnd))
    acceptState.isAccept = false
    startState = newStart
    acceptState = newEnd
  }

  /**
   * Marks the start and accept states of the NFA to start
   * and end, respectfully, the capturing of a group.
   *
   * @param groupNumber The number of the capture group
   */
  internal fun capture(groupNumber: Int, force: Boolean = true) {
    this.startState.startCapture.add(groupNumber)
    if (force) this.acceptState.forceEndCapture.add(groupNumber)
    else this.acceptState.endCapture.add(groupNumber)
  }

  internal fun assert(shouldConsume: Boolean, isPositive: Boolean = false) : NFA {
    val newStart = NFAState(false)
    val newEnd = NFAState(true)

    newStart.assertion = NFAAssertion(
      shouldConsume,
      isPositive,
      startState,
      acceptState,
      newEnd
    )

    acceptState.isAccept = false
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

  /**
   * Simulates the nfa in depth-first search fashion.
   *
   * @param editor       The editor that is used for the simulation
   * @param startIndex   The index where the simulation should start
   *
   * @return The resulting match result
   */
  internal fun simulate(editor: VimEditor, startIndex: Int = 0, isCaseInsensitive: Boolean = false) : VimMatchResult {
    groups.groupCount = 0
    if (simulate(editor, startIndex, startState, isCaseInsensitive)) {
      return groups.get(0)?.let {
        VimMatchResult.Success(
          it.range,
          it.value,
          groups
        )
      } ?: run { VimMatchResult.Failure }
    }
    return VimMatchResult.Failure
  }

  /**
   * Simulates the NFA in a depth-first search fashion.
   *
   * @param editor            The editor that is used for the simulation
   * @param currentIndex      The current index of the text in the simulation
   * @param currentState      The current NFA state in the simulation
   * @param isCaseInsensitive Whether the simulation should ignore case
   * @param epsilonVisited    Records the states that have been visited up to this point without consuming any input
   *
   * @return True if matching was successful, false otherwise
   */
  private fun simulate(
    editor: VimEditor,
    currentIndex: Int,
    currentState: NFAState,
    isCaseInsensitive: Boolean,
    epsilonVisited: Set<NFAState> = HashSet()
  ): Boolean {
    if (handleAssertion(editor, currentIndex, currentState, isCaseInsensitive)) return true

    updateCaptureGroups(editor, currentIndex, currentState)

    if (currentState.isAccept) return true

    for (transition in currentState.transitions) {
      if (handleTransition(editor, currentIndex, currentState, isCaseInsensitive, transition, epsilonVisited)) {
        return true
      }
    }

    return false
  }

  private fun handleAssertion(
    editor: VimEditor,
    currentIndex: Int,
    currentState: NFAState,
    isCaseInsensitive: Boolean
  ): Boolean {
    val assertion = currentState.assertion ?: return false

    val assertionResult = assertion.assert(editor, currentIndex, isCaseInsensitive, groups)
    if (assertionResult.assertionSuccess != assertion.isPositive) return false

    val newIndex = if (assertion.shouldConsume) assertionResult.index else currentIndex
    return simulate(editor, newIndex, assertion.jumpTo, isCaseInsensitive)
  }

  private fun handleTransition(
    editor: VimEditor,
    currentIndex: Int,
    currentState: NFAState,
    isCaseInsensitive: Boolean,
    transition: NFATransition,
    epsilonVisited: Set<NFAState>
  ): Boolean {
    val transitionMatcherResult = transition.matcher.matches(editor, currentIndex, groups, isCaseInsensitive)
    if (transitionMatcherResult !is MatcherResult.Success) return false

    val nextIndex = currentIndex + transitionMatcherResult.consumed
    val destState = transition.destState

    if (transitionMatcherResult.consumed == 0 && epsilonVisited.contains(destState)) {
      return false
    }

    val epsilonVisitedCopy = if (transitionMatcherResult.consumed == 0 && !epsilonVisited.contains(destState)) {
      epsilonVisited.plusElement(currentState)
    } else {
      HashSet()
    }
    return simulate(editor, nextIndex, destState, isCaseInsensitive, epsilonVisitedCopy)
  }

  /**
   * Updates the results of capture groups' matches
   *
   * @param editor The editor that is used for the simulation
   * @param index  The current index of the text in the simulation
   * @param state  The current state in the simulation
   */
  private fun updateCaptureGroups(editor: VimEditor, index: Int, state: NFAState) {
    for (groupNumber in state.startCapture) groups.setGroupStart(groupNumber, index)
    for (groupNumber in state.endCapture) groups.setGroupEnd(groupNumber, index, editor.text())
    for (groupNumber in state.forceEndCapture) groups.setForceGroupEnd(groupNumber, index, editor.text())
  }

  internal companion object {

    /**
     * Creates a new instance of a NFA, that has a single
     * state.
     *
     * @return THe new NFA instance
     */
    internal fun fromSingleState() : NFA {
      val state = NFAState(true)
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
    internal fun fromMatcher(matcher: Matcher) : NFA {
      val startState = NFAState(false)
      val acceptState = NFAState(true)

      startState.addTransition(NFATransition(matcher, acceptState))
      return NFA(startState, acceptState)
    }
  }
}

private data class SimulationStackFrame(val index: Int, val state: NFAState, val epsilonVisited: HashSet<NFAState>)