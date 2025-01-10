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
import com.maddyhome.idea.vim.regexp.engine.nfa.NFAState
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.BackreferenceMatcher
import com.maddyhome.idea.vim.regexp.engine.nfa.matcher.MatcherResult
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection
import com.maddyhome.idea.vim.regexp.match.VimMatchResult

internal class ImplicitDFAStrategy : SimulationStrategy {
  override fun simulate(nfa: NFA, editor: VimEditor, startIndex: Int, isCaseInsensitive: Boolean): SimulationResult {
    val possibleCursors = editor.carets().toMutableList()
    var currentStates = mutableListOf(nfa.startState)
    val groups = VimMatchGroupCollection()

    for (index in startIndex..editor.text().length) {
      val epsilonClosures = currentStates.flatMap { state ->
        epsilonClosure(
          editor,
          index,
          isCaseInsensitive,
          groups,
          possibleCursors,
          state
        )
      }
      val nextStates = mutableListOf<NFAState>()
      for (state in epsilonClosures) {

        // if there is anything that the algorithm can't deal with, we can't know for sure whether there is a match or not
        if (state.assertion != null ||
          state.hasLazyMulti ||
          state.transitions.any { it.matcher is BackreferenceMatcher }
        ) return SimulationResult.Incomplete

        nextStates.addAll(state.transitions.filter {
          !it.matcher.isEpsilon() &&
            it.matcher.matches(editor, index, groups, isCaseInsensitive, possibleCursors) is MatcherResult.Success
        }.map { it.destState })
      }
      if (nextStates.isEmpty()) break
      currentStates = nextStates
    }
    groups.get(0)?.let {
      return SimulationResult.Complete(VimMatchResult.Success(it.range, it.value, groups))
    }
    return SimulationResult.Complete(VimMatchResult.Failure(VimRegexErrors.E486))
  }

  private fun epsilonClosure(
    editor: VimEditor,
    index: Int,
    isCaseInsensitive: Boolean,
    groups: VimMatchGroupCollection,
    possibleCursors: MutableList<VimCaret>,
    state: NFAState,
    visited: MutableSet<NFAState> = mutableSetOf(),
  ): List<NFAState> {
    updateCaptureGroups(editor, index, state, groups)
    if (!state.transitions.any { it.matcher.isEpsilon() }) return listOf(state)

    val result = mutableListOf<NFAState>()
    for (transition in state.transitions.filter { it.matcher.isEpsilon() }) {
      if (visited.contains(transition.destState) || transition.matcher.matches(
          editor,
          index,
          groups,
          isCaseInsensitive,
          possibleCursors
        ) is MatcherResult.Failure
      ) continue
      visited.add(transition.destState)
      result.addAll(
        epsilonClosure(
          editor,
          index,
          isCaseInsensitive,
          groups,
          possibleCursors,
          transition.destState,
          visited
        )
      )
    }
    return result
  }

  /**
   * Updates the results of capture groups' matches
   *
   * @param editor The editor that is used for the simulation
   * @param index  The current index of the text in the simulation
   * @param state  The current state in the simulation
   */
  private fun updateCaptureGroups(editor: VimEditor, index: Int, state: NFAState, groups: VimMatchGroupCollection) {
    for (groupNumber in state.startCapture) groups.setGroupStart(groupNumber, index)
    for (groupNumber in state.endCapture) groups.setGroupEnd(groupNumber, index, editor.text())
    for (groupNumber in state.forceEndCapture) groups.setForceGroupEnd(groupNumber, index, editor.text())
  }
}