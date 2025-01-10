/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.engine

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.VimRegexErrors
import com.maddyhome.idea.vim.regexp.engine.nfa.NFA
import com.maddyhome.idea.vim.regexp.engine.strategies.BacktrackingStrategy
import com.maddyhome.idea.vim.regexp.engine.strategies.SimulationResult
import com.maddyhome.idea.vim.regexp.engine.strategies.SimulationStrategy
import com.maddyhome.idea.vim.regexp.match.VimMatchResult

/**
 * A meta-engine for simulating a nfa. It combines strategies that can be used to simulate the nfa,
 * some more powerful but slower, others less powerful but faster. The engine combines these strategies,
 * in order to always use the strategy that is less powerful (and thus faster), but powerful enough to
 * simulate the nfa.
 * This is a singleton.
 */
internal object VimRegexEngine {

  // TODO: optimize by adding more strategies. The strategies should go from less powerful but faster, to more powerful but slower
  /**
   * The list of strategies that the engine has available. They should be ordered from less powerful to more powerful.
   */
  private val strategies: List<SimulationStrategy> = listOf(BacktrackingStrategy())

  /**
   * Simulate the nfa using the available strategies. The approach used is very simple: start with the least powerful
   * strategy; if this strategy is powerful enough to determine if there is a match, return that match. If it isn't
   * powerful enough, use the next (more powerful) strategy.
   */
  internal fun simulate(
    nfa: NFA,
    editor: VimEditor,
    startIndex: Int = 0,
    isCaseInsensitive: Boolean = false,
  ): VimMatchResult {
    for (strategy in strategies) {
      val result = strategy.simulate(nfa, editor, startIndex, isCaseInsensitive)
      if (result is SimulationResult.Complete) return result.matchResult
    }
    return VimMatchResult.Failure(VimRegexErrors.E486)
  }
}