/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa

import com.maddyhome.idea.vim.regexp.nfa.matcher.Matcher

/**
 * Represents a transition of the NFA.
 *
 * @param matcher The matcher that determines if the transition can
 * be made, as well as information on how many characters
 * are consumed by the transition.
 *
 * @param destState The destination state of the transition.
 */
internal data class NFATransition(
  val matcher: Matcher,
  val destState: NFAState,
)