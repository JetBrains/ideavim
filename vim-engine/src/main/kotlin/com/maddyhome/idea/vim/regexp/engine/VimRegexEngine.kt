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
import com.maddyhome.idea.vim.regexp.engine.strategies.SimulationStrategy
import com.maddyhome.idea.vim.regexp.match.VimMatchResult

internal object VimRegexEngine {

  // TODO: optimize by adding more strategies. The strategies should go from less powerful but faster, to more powerful but slower
  private val strategies: List<SimulationStrategy> = listOf(BacktrackingStrategy())

  internal fun simulate(nfa: NFA, editor: VimEditor, startIndex: Int = 0, isCaseInsensitive: Boolean = false): VimMatchResult {
    for (strategy in strategies) {
      val result = strategy.simulate(nfa, editor, startIndex, isCaseInsensitive)
      if (result is SimulationResult.Complete) return result.matchResult
    }
    // should never reach here, since at least one strategy should be powerful enough to complete the simulation
    return VimMatchResult.Failure(VimRegexErrors.E486)
  }
}