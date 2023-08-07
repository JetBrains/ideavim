/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.nfa.NFAState

/**
 * Matcher that always matches. It is used to represent
 * epsilon transitions. This transitions can always be
 * taken and without consuming any character.
 */
internal class EpsilonMatcher : Matcher {
  override fun matches(editor: VimEditor, index: Int): Boolean {
    return true
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}