/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa

/**
 * Represents an assertion.
 *
 * @param shouldConsume Whether the simulation should consume the input "consumed" by the assertion.
 * @param isPositive    True if the assertion is positive, false if negative.
 * @param startState    The state to jump to, to start the assertion
 * @param endState      The state where the assertion should end
 * @param jumpTo        The state that the simulation should jump to, to resume with normal
 * @param limit         Only relevant for lookbehinds. Determines how far back to look for assertion
 * simulation after the assertion.
 *
 * @see :help /@=
 * @see :help /@!
 * @see :help /@&lt=
 * @see :help /@&lt!
 * @see :help /@&gt
 */
internal data class NFAAssertion(
  val shouldConsume: Boolean,
  val isPositive: Boolean,
  val isAhead: Boolean,
  val startState: NFAState,
  val endState: NFAState,
  val jumpTo: NFAState,
  val limit: Int,
)