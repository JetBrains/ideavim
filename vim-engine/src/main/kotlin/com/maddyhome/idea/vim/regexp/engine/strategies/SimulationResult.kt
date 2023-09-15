/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.engine.strategies

import com.maddyhome.idea.vim.regexp.match.VimMatchResult

/**
 * The result of applying a SimulationStrategy.
 */
internal sealed class SimulationResult {
  /**
   * The simulation is deemed "complete" if it found a match, or if
   * it can determine with absolute certainty there are no matches.
   */
  data class Complete(val matchResult: VimMatchResult) : SimulationResult()

  /**
   * The simulation is deemed "incomplete" if it just isn't powerful
   * enough to determine whether there is match.
   */
  object Incomplete : SimulationResult()
}