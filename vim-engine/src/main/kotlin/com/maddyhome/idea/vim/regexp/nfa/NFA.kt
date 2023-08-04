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
import com.maddyhome.idea.vim.regexp.nfa.matcher.LoopMatcher
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
   * Loops the NFA. The NFA must be transversed at least n times
   * but no more than m. m can be infinite.
   *
   * @param n The lowest amount of times that the NFA must be transversed
   * @param m The highest amount of times the NFA can be transversed
   */
  internal fun loop(n: MultiDelimiter.IntMultiDelimiter, m: MultiDelimiter) : NFA {
    val newStart = NFAState(false)
    val newEnd = NFAState(true)

    /**
     * This transition indicates the beginning of a loop.
     * It initializes the loop counter variable of the first
     * state in the loop.
     */
    val initLoopTransition = NFATransition(
      EpsilonMatcher(),
      this.startState
    ) { state -> state.i = 0 }
    newStart.addTransition(initLoopTransition)

    /**
     * This transition indicates that the NFA is looping back
     * to its start. Increments the loop counter variable.
     */
    val incLoopTransition = NFATransition(
      EpsilonMatcher(),
      this.startState
    ) { state -> state.i = state.i + 1 }
    this.acceptState.addTransition(incLoopTransition)

    /**
     * This transition is used to exit out of the loop.
     */
    val exitLoopTransition = NFATransition(
      LoopMatcher(n, m),
      newEnd
    )
    this.startState.addTransition(exitLoopTransition)

    this.acceptState.isAccept = false
    this.startState = newStart
    this.acceptState = newEnd

    return this
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

  internal fun simulate(editor: VimEditor, startIndex: Int = 0) : VimMatchResult {
    groups.groupCount = 0
    if (simulate(editor, startIndex, startIndex, startState)) {
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
   * @param editor       The editor that is used for the simulation
   * @param startIndex   The index of the text in the editor where the simulation should start at
   * @param currentIndex The current index of the text in the simulation
   * @param currentState The current NFA state in the simulation
   *
   * @return The resulting match if it was found, else null
   */
  private fun simulate(editor: VimEditor, startIndex : Int = 0, currentIndex : Int = startIndex, currentState: NFAState = startState) : Boolean {
    updateCaptureGroups(editor, currentIndex, currentState)
    if (currentState.isAccept) return true
    for (transition in currentState.transitions) {
      val newIndex = currentIndex + transition.consumes()
      if (transition.canTake(editor, currentIndex, currentState)) {
        transition.takeAction()
        if (simulate(editor, startIndex, newIndex, transition.destState)) return true
      }
    }
    currentState.i--
    return false
  }

  private fun updateCaptureGroups(editor: VimEditor, index: Int, state: NFAState) {
    for (groupNumber in state.startCapture) groups.setGroupStart(groupNumber, index)
    for (groupNumber in state.endCapture) groups.setGroupEnd(groupNumber, index, editor.text())
  }

  internal companion object {

    /**
     * Creates a new instance of a NFA, that has two states
     * with an epsilon transition from one to the other.
     *
     * start --ε-> end
     *
     * @return The new NFA instance
     */
    internal fun fromEpsilon() : NFA {
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
    internal fun fromMatcher(matcher: Matcher) : NFA {
      val startState = NFAState(false)
      val acceptState = NFAState(true)

      startState.addTransition(NFATransition(matcher, acceptState))
      return NFA(startState, acceptState)
    }
  }
}