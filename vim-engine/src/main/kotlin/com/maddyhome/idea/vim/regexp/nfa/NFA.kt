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
   * @return The new NFA representing the closure
   */
  internal fun closure() : NFA {
    val newStart = NFAState(false)
    val newEnd = NFAState(true)

    newStart.addTransition(NFATransition(EpsilonMatcher(), startState))
    newStart.addTransition(NFATransition(EpsilonMatcher(), newEnd))

    acceptState.addTransition(NFATransition(EpsilonMatcher(), startState))
    acceptState.addTransition(NFATransition(EpsilonMatcher(), newEnd))

    acceptState.isAccept = false
    startState = newStart
    acceptState = newEnd

    return this
  }

  /**
   * Gives the NFA the choice to jump directly from its start to
   * accept state, without taking any of the inner transitions.
   */
  internal fun optional() {
    startState.addTransition(NFATransition(EpsilonMatcher(), acceptState))
  }

  /**
   * Marks the start and accept states of the NFA to start
   * and end, respectfully, the capturing of a group.
   *
   * @param groupNumber The number of the capture group
   */
  internal fun capture(groupNumber: Int) {
    this.startState.startCapture.add(groupNumber)
    this.acceptState.endCapture.add(groupNumber)
  }

  /**
   * Simulates the nfa in depth-first search fashion.
   *
   * @param editor       The editor that is used for the simulation
   * @param startIndex   The index where the simulation should start
   *
   * @return The resulting match result
   */
  internal fun simulate(editor: VimEditor, startIndex: Int = 0) : VimMatchResult {
    groups.groupCount = 0
    if (simulate(editor, startIndex, startState)) {
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
   * @param editor         The editor that is used for the simulation
   * @param currentIndex   The current index of the text in the simulation
   * @param currentState   The current NFA state in the simulation
   * @param epsilonVisited Records the states that have been visited up to this point without consuming any input
   *
   * @return True if matching was successful, false otherwise
   */
  private fun simulate(editor: VimEditor, currentIndex : Int = 0, currentState: NFAState = startState, epsilonVisited: HashSet<NFAState> = HashSet()) : Boolean {
    updateCaptureGroups(editor, currentIndex, currentState)
    if (currentState.isAccept) return true
    for (transition in currentState.transitions) {
      val newIndex = currentIndex + transition.consumes()
      var epsilonVisitedCopy = HashSet(epsilonVisited)
      if (transition.isEpsilon()) {
        if (epsilonVisited.contains(transition.destState)) continue
        epsilonVisitedCopy.add(currentState)
      } else {
        epsilonVisitedCopy = HashSet()
      }
      if (transition.canTake(editor, currentIndex)) {
        if (simulate(editor, newIndex, transition.destState, epsilonVisitedCopy)) return true
      }
    }
    return false
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