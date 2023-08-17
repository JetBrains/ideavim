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
import com.maddyhome.idea.vim.regexp.nfa.matcher.MatcherResult

internal class NFAAssertion(
  val shouldConsume: Boolean,
  val isPositive: Boolean,
  private val startState: NFAState,
  private val endState: NFAState,
  val jumpTo: NFAState
) {
  internal fun assert(
    editor: VimEditor,
    index: Int,
    isCaseInsensitive: Boolean,
    groups: VimMatchGroupCollection
  ) : NFAAssertionResult {
    return simulate(editor, index, startState, isCaseInsensitive, groups)
  }

  private fun simulate(
    editor: VimEditor,
    currentIndex: Int,
    currentState: NFAState,
    isCaseInsensitive: Boolean,
    groups: VimMatchGroupCollection,
    epsilonVisited: HashSet<NFAState> = HashSet(),
  ) : NFAAssertionResult {
    updateCaptureGroups(editor, currentIndex, currentState, groups)
    if (currentState === endState) return NFAAssertionResult(true, currentIndex)
    for (transition in currentState.transitions) {
      val transitionMatcherResult = transition.matcher.matches(editor, currentIndex, groups, isCaseInsensitive)
      if (transitionMatcherResult is MatcherResult.Success) {
        var epsilonVisitedCopy = HashSet(epsilonVisited)
        if (transitionMatcherResult.consumed == 0) {
          if (epsilonVisited.contains(transition.destState)) continue
          epsilonVisitedCopy.add(currentState)
        } else {
          epsilonVisitedCopy = HashSet()
        }
        val result = simulate(editor, currentIndex + transitionMatcherResult.consumed, transition.destState, isCaseInsensitive, groups, epsilonVisitedCopy)
        if (result.assertionSuccess) return result
      }
    }
    return NFAAssertionResult(false, currentIndex)
  }

  private fun updateCaptureGroups(editor: VimEditor, index: Int, state: NFAState, groups: VimMatchGroupCollection) {
    for (groupNumber in state.startCapture) groups.setGroupStart(groupNumber, index)
    for (groupNumber in state.endCapture) groups.setGroupEnd(groupNumber, index, editor.text())
    for (groupNumber in state.forceEndCapture) groups.setForceGroupEnd(groupNumber, index, editor.text())
  }
}

internal data class NFAAssertionResult(
  val assertionSuccess: Boolean,
  val index: Int
)