/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.engine

import com.maddyhome.idea.vim.regexp.match.VimMatchResult

internal sealed class SimulationResult {
  data class Complete(val matchResult: VimMatchResult) : SimulationResult()
  object Incomplete : SimulationResult()
}