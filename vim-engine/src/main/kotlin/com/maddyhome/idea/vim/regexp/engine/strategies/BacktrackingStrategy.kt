/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.engine.strategies

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.VimRegexErrors
import com.maddyhome.idea.vim.regexp.engine.nfa.NFA
import com.maddyhome.idea.vim.regexp.engine.nfa.NFAAssertion
import com.maddyhome.idea.vim.regexp.engine.nfa.NFAState
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.MatcherResult
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection
import com.maddyhome.idea.vim.regexp.match.VimMatchResult
import kotlin.math.max

/**
 * Uses a backtracking based strategy to simulate the nfa. This strategy is very powerful, since it
 * can be used with any nfa, but comes at the cost of speed.
 */
internal class BacktrackingStrategy : SimulationStrategy {

  /**
   * Memory used to store capture groups
   */
  private val groups: VimMatchGroupCollection = VimMatchGroupCollection()

  override fun simulate(nfa: NFA, editor: VimEditor, startIndex: Int, isCaseInsensitive: Boolean): SimulationResult {
    groups.clear()
    if (simulate(
        editor,
        startIndex,
        nfa.startState,
        nfa.acceptState,
        isCaseInsensitive,
        editor.carets().toMutableList()
      ).simulationResult
    ) {
      return SimulationResult.Complete(
        groups.get(0)?.let {
          VimMatchResult.Success(
            it.range,
            it.value,
            groups
          )
        } ?: run { VimMatchResult.Failure(VimRegexErrors.E486) }
      )
    }
    return SimulationResult.Complete(VimMatchResult.Failure(VimRegexErrors.E486))
  }

  /**
   * Simulates the NFA in a depth-first search fashion.
   *
   * @param editor            The editor that is used for the simulation
   * @param index             The current index of the text in the simulation
   * @param state             The current NFA state in the simulation
   * @param targetState       The NFA state that needs to be found for a successful match
   * @param isCaseInsensitive Whether the simulation should ignore case
   * @param maxIndex          The maximum index of the text that the simulation is allowed to go to
   * @param possibleCursors   The cursors that are allowed to match
   *
   * @return The result of the simulation. It tells whether it was successful, and at what index it stopped.
   */
  private fun simulate(
    editor: VimEditor,
    index: Int,
    state: NFAState,
    targetState: NFAState,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
    maxIndex: Int = editor.text().length,
  ): NFASimulationResult {
    val stack = mutableListOf<SimulationStackFrame>()
    stack.add(SimulationStackFrame(index, state, NfaStateList.empty))

    while (stack.isNotEmpty()) {
      val currFrame = stack.removeLast()
      if (currFrame.currentIndex > maxIndex) continue
      updateCaptureGroups(editor, currFrame.currentIndex, currFrame.currentState)
      if (currFrame.currentState === targetState) return NFASimulationResult(true, currFrame.currentIndex)
      currFrame.currentState.assertion?.let {
        val assertionResult = handleAssertion(editor, currFrame.currentIndex, isCaseInsensitive, it, possibleCursors)
        if (assertionResult.simulationResult) stack.add(
          SimulationStackFrame(
            assertionResult.index,
            currFrame.currentState.assertion!!.jumpTo,
            NfaStateList.empty
          )
        )
      }


      for (i in currFrame.currentState.transitions.lastIndex downTo 0) {
        val transition = currFrame.currentState.transitions[i]
        val transitionMatcherResult =
          transition.matcher.matches(editor, currFrame.currentIndex, groups, isCaseInsensitive, possibleCursors)
        if (transitionMatcherResult !is MatcherResult.Success) continue
        val destState = transition.destState
        if (transitionMatcherResult.consumed == 0 && currFrame.epsilonVisited.contains(destState)) continue
        val nextIndex = currFrame.currentIndex + transitionMatcherResult.consumed
        val epsilonVisitedCopy =
          if (transitionMatcherResult.consumed == 0 && !currFrame.epsilonVisited.contains(destState)) NfaStateList(
            currFrame.currentState,
            currFrame.epsilonVisited
          ) else NfaStateList.empty
        stack.add(SimulationStackFrame(nextIndex, destState, epsilonVisitedCopy))
      }
    }
    return NFASimulationResult(false, index)
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
    possibleCursors: MutableList<VimCaret>,
  ): NFASimulationResult {
    return if (assertion.isAhead) handleAheadAssertion(
      editor,
      currentIndex,
      isCaseInsensitive,
      assertion,
      possibleCursors
    )
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
    possibleCursors: MutableList<VimCaret>,
  ): NFASimulationResult {
    val assertionResult =
      simulate(editor, currentIndex, assertion.startState, assertion.endState, isCaseInsensitive, possibleCursors)
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
    possibleCursors: MutableList<VimCaret>,
  ): NFASimulationResult {
    var lookBehindStartIndex = currentIndex - 1
    val minIndex = if (assertion.limit == 0) 0 else max(0, currentIndex - assertion.limit)
    var seenNewLine = false
    while (lookBehindStartIndex >= minIndex && !(seenNewLine && editor.text()[lookBehindStartIndex] != '\n')) {
      // the lookbehind is allowed to look back as far as to the start of the previous line
      if (editor.text()[lookBehindStartIndex] == '\n') seenNewLine = true

      val result = simulate(
        editor,
        lookBehindStartIndex,
        assertion.startState,
        assertion.endState,
        isCaseInsensitive,
        possibleCursors,
        maxIndex = currentIndex
      )
      // found a match that ends before the "currentIndex"
      if (result.simulationResult && result.index == currentIndex) {
        return if (assertion.isPositive) NFASimulationResult(
          true,
          currentIndex
        )
        else NFASimulationResult(false, currentIndex)
      }
      lookBehindStartIndex--
    }
    return if (assertion.isPositive) NFASimulationResult(false, currentIndex)
    else NFASimulationResult(true, currentIndex)
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
}

/**
 * Represents the result of simulating a NFA
 */
private data class NFASimulationResult(
  /**
   * Whether the simulation reached a target state successfully
   */
  val simulationResult: Boolean,

  /**
   * The index of the input editor text at which the simulation stopped
   */
  val index: Int,
)

private data class SimulationStackFrame(
  val currentIndex: Int,
  val currentState: NFAState,
  val epsilonVisited: NfaStateList,
)

private class NfaStateList(
  val item: NFAState,
  val tail: NfaStateList?,
) {
  fun contains(item: NFAState): Boolean {
    return this.item == item || this.tail?.contains(item) == true
  }

  companion object {
    val empty = NfaStateList(NFAState(), null)
  }
}
