/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.engine.strategies

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.engine.SimulationResult
import com.maddyhome.idea.vim.regexp.engine.nfa.NFA

internal interface SimulationStrategy {

  /**
   * Simulates a nfa using some strategy
   *
   * @param nfa               The nfa to simulate
   * @param editor            The editor that is used for the simulation
   * @param startIndex        The index where the simulation should start
   * @param isCaseInsensitive Whether the simulation should ignore case
   *
   * @return The resulting match result
   */
  fun simulate(nfa: NFA, editor: VimEditor, startIndex: Int, isCaseInsensitive: Boolean): SimulationResult
}