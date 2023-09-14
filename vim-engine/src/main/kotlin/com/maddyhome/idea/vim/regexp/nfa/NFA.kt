/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.VimRegexErrors
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import com.maddyhome.idea.vim.regexp.nfa.matcher.EpsilonMatcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.Matcher
import com.maddyhome.idea.vim.regexp.nfa.matcher.MatcherResult
import kotlin.math.max

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
  internal fun closure(isGreedy: Boolean) : NFA {
    val newStart = NFAState()
    val newEnd = NFAState()

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
  internal fun optional(isGreedy: Boolean) : NFA {
    val newStart = NFAState()
    val newEnd = NFAState()

    if (isGreedy) {
      newStart.addTransition(NFATransition(EpsilonMatcher(), startState))
      newStart.addTransition(NFATransition(EpsilonMatcher(), newEnd))
    }
    else {
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
   * @param isPositive Whether the assertion is positive or negative.
   *
   * @return The NFA instance marked for assertion.
   */
  internal fun assert(shouldConsume: Boolean, isPositive: Boolean, isAhead: Boolean, limit: Int = 0) : NFA {
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

  /**
   * Simulates the nfa in depth-first search fashion.
   *
   * @param editor            The editor that is used for the simulation
   * @param startIndex        The index where the simulation should start
   * @param isCaseInsensitive Whether the simulation should ignore case
   *
   * @return The resulting match result
   */
  internal fun simulate(editor: VimEditor, startIndex: Int = 0, isCaseInsensitive: Boolean = false) : VimMatchResult {
    groups.groupCount = 0
    if (simulate(editor, startIndex, startState, acceptState, isCaseInsensitive, editor.carets().toMutableList()).simulationResult) {
      return groups.get(0)?.let {
        VimMatchResult.Success(
          it.range,
          it.value,
          groups
        )
      } ?: run { VimMatchResult.Failure(VimRegexErrors.E486) }
    }
    return VimMatchResult.Failure(VimRegexErrors.E486)
  }

  /**
   * Simulates the NFA in a depth-first search fashion.
   *
   * @param editor            The editor that is used for the simulation
   * @param currentIndex      The current index of the text in the simulation
   * @param currentState      The current NFA state in the simulation
   * @param targetState       The NFA state that needs to be found for a successful match
   * @param isCaseInsensitive Whether the simulation should ignore case
   * @param epsilonVisited    Records the states that have been visited up to this point without consuming any input
   * @param maxIndex          The maximum index of the text that the simulation is allowed to go to
   * @param possibleCursors   The cursors that are allowed to match
   *
   * @return The result of the simulation. It tells whether it was successful, and at what index it stopped.
   */
  private fun simulate(
    editor: VimEditor,
    currentIndex: Int,
    currentState: NFAState,
    targetState: NFAState,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
    epsilonVisited: Set<NFAState> = HashSet(),
    maxIndex: Int = editor.text().length
  ): NFASimulationResult {
    if (currentIndex > maxIndex) return NFASimulationResult(false, currentIndex)

    updateCaptureGroups(editor, currentIndex, currentState)
    currentState.assertion?.let {
      val assertionResult = handleAssertion(editor, currentIndex, isCaseInsensitive, it, possibleCursors)
      if (!assertionResult.simulationResult) return NFASimulationResult(false, currentIndex)
      else return simulate(editor, assertionResult.index, currentState.assertion!!.jumpTo, targetState, isCaseInsensitive, possibleCursors, maxIndex=maxIndex)
    }
    if (currentState === targetState) return NFASimulationResult(true, currentIndex)

    for (transition in currentState.transitions) {
      val transitionResult = handleTransition(editor, currentIndex, currentState, targetState, isCaseInsensitive, transition, epsilonVisited, maxIndex, possibleCursors)
      if (transitionResult.simulationResult) return transitionResult
    }
    return NFASimulationResult(false, currentIndex)
  }

  /**
   * Handles a state of the NFA that has an assertion. Determines if the assertion
   * was successful or not, and where the normal simulation should resume.
   *
   * @param editor            The editor that is used for the simulation
   * @param currentIndex      The current index of the text in the simulation
   * @param isCaseInsensitive Whether the simulation should ignore case
   * @param assertion         The assertion that is to be handled
   * @param possibleCursors   The cursors that are allowed to match
   *
   * @return The result of the assertion. It tells whether it was successful, and at what index it stopped.
   */
  private fun handleAssertion(
    editor: VimEditor,
    currentIndex: Int,
    isCaseInsensitive: Boolean,
    assertion: NFAAssertion,
    possibleCursors: MutableList<VimCaret>
  ): NFASimulationResult {
    return if (assertion.isAhead) handleAheadAssertion(editor, currentIndex, isCaseInsensitive, assertion, possibleCursors)
    else handleBehindAssertion(editor, currentIndex, isCaseInsensitive, assertion, possibleCursors)
  }

  /**
   * Handles a state of the NFA that has an assertion ahead. Determines if the assertion
   * was successful or not, and where the normal simulation should resume.
   *
   * @param editor            The editor that is used for the simulation
   * @param currentIndex      The current index of the text in the simulation
   * @param isCaseInsensitive Whether the simulation should ignore case
   * @param assertion         The assertion that is to be handled
   * @param possibleCursors   The cursors that are allowed to match
   *
   * @return The result of the assertion. It tells whether it was successful, and at what index it stopped.
   */
  private fun handleAheadAssertion(
    editor: VimEditor,
    currentIndex: Int,
    isCaseInsensitive: Boolean,
    assertion: NFAAssertion,
    possibleCursors: MutableList<VimCaret>
  ): NFASimulationResult {
    val assertionResult = simulate(editor, currentIndex, assertion.startState, assertion.endState, isCaseInsensitive, possibleCursors)
    if (assertionResult.simulationResult != assertion.isPositive) {
      return NFASimulationResult(false, currentIndex)
    }

    /**
     * If the assertion should consume input, the normal simulation resumes at the index where the
     * assertion stopped, else it resumes at the index that the simulation was at before the assertion.
     */
    val newIndex = if (assertion.shouldConsume) assertionResult.index else currentIndex
    return NFASimulationResult(true, newIndex)
  }

  /**
   * Handles a state of the NFA that has an assertion behind. Determines if the assertion
   * was successful or not, and where the normal simulation should resume.
   *
   * @param editor            The editor that is used for the simulation
   * @param currentIndex      The current index of the text in the simulation
   * @param isCaseInsensitive Whether the simulation should ignore case
   * @param assertion         The assertion that is to be handled
   * @param possibleCursors   The cursors that are allowed to match
   *
   * @return The result of the assertion. It tells whether it was successful, and at what index it stopped.
   */
  private fun handleBehindAssertion(
    editor: VimEditor,
    currentIndex: Int,
    isCaseInsensitive: Boolean,
    assertion: NFAAssertion,
    possibleCursors: MutableList<VimCaret>
  ): NFASimulationResult {
    var lookBehindStartIndex = currentIndex - 1
    val minIndex = if (assertion.limit == 0) 0 else max(0, currentIndex - assertion.limit)
    var seenNewLine = false
    while (lookBehindStartIndex >= minIndex && !(seenNewLine && editor.text()[lookBehindStartIndex] != '\n')) {
      // the lookbehind is allowed to look back as far as to the start of the previous line
      if (editor.text()[lookBehindStartIndex] == '\n') seenNewLine = true

      val result = simulate(editor, lookBehindStartIndex, assertion.startState, assertion.endState, isCaseInsensitive, possibleCursors, maxIndex = currentIndex)
      // found a match that ends before the "currentIndex"
      if (result.simulationResult && result.index == currentIndex) {
        return if (assertion.isPositive) NFASimulationResult(true, currentIndex)
        else NFASimulationResult(false, currentIndex)
      }
      lookBehindStartIndex--
    }
    return if (assertion.isPositive) NFASimulationResult(false, currentIndex)
    else NFASimulationResult(true, currentIndex)
  }

  /**
   * Tries to take a transition, and continues the simulation from the destination state
   * of said transition.
   *
   * @param editor            The editor that is used for the simulation
   * @param currentIndex      The current index of the text in the simulation
   * @param currentState      The current NFA state in the simulation
   * @param targetState       The NFA state that needs to be found for a successful match
   * @param isCaseInsensitive Whether the simulation should ignore case
   * @param transition        The transition that is to be handled
   * @param epsilonVisited    Records the states that have been visited up to this point without consuming any input
   * @param maxIndex          The maximum index of the text that the simulation is allowed to go to
   * @param possibleCursors   The cursors that are allowed to match
   *
   * @return The result of taking the transition. It tells whether it was successful, and at what index it stopped.
   */
  private fun handleTransition(
    editor: VimEditor,
    currentIndex: Int,
    currentState: NFAState,
    targetState: NFAState,
    isCaseInsensitive: Boolean,
    transition: NFATransition,
    epsilonVisited: Set<NFAState>,
    maxIndex: Int,
    possibleCursors: MutableList<VimCaret>
  ): NFASimulationResult {
    val transitionMatcherResult = transition.matcher.matches(editor, currentIndex, groups, isCaseInsensitive, possibleCursors)
    if (transitionMatcherResult !is MatcherResult.Success) return NFASimulationResult(false, currentIndex)

    val nextIndex = currentIndex + transitionMatcherResult.consumed
    val destState = transition.destState

    if (transitionMatcherResult.consumed == 0 && epsilonVisited.contains(destState)) {
      return NFASimulationResult(false, currentIndex)
    }

    val epsilonVisitedCopy = if (transitionMatcherResult.consumed == 0 && !epsilonVisited.contains(destState)) {
      epsilonVisited.plusElement(currentState)
    } else {
      HashSet()
    }
    return simulate(editor, nextIndex, destState, targetState, isCaseInsensitive, possibleCursors, epsilonVisitedCopy, maxIndex)
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
    internal fun fromMatcher(matcher: Matcher) : NFA {
      val startState = NFAState()
      val acceptState = NFAState()

      startState.addTransition(NFATransition(matcher, acceptState))
      return NFA(startState, acceptState)
    }
  }
}

/**
 * Represents the result of simulating a NFA
 */
private data class NFASimulationResult (
  /**
   * Whether the simulation reached a target state successfully
   */
  val simulationResult: Boolean,

  /**
   * The index of the input editor text at which the simulation stopped
   */
  val index: Int
)
