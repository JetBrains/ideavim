/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.nfa.matcher.Matcher

internal class NFATransition(
  val matcher: Matcher,
  val destState: NFAState,
  val action: (state: NFAState) -> Unit = {}
) {

  fun canTake(editor: VimEditor, index: Int, state: NFAState) : Boolean {
    return matcher.matches(editor, index, state)
  }

  fun takeAction() {
    action(destState)
  }

  fun consumes() : Int {
    return if (matcher.isEpsilon()) 0 else 1
  }
}