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

/**
 * Represents a transition of the NFA
 */
internal class NFATransition(
  /**
   * The matcher that determines if the transition can
   * be made, as well as information on how many characters
   * are consumed by the transition
   */
  val matcher: Matcher,

  /**
   * The destination state of the transition
   */
  val destState: NFAState,
) {

  /**
   * Determines whether the transition can be made
   *
   * @param editor The editor in its current state
   * @param index  The current index in the text of the editor
   *
   * @return Whether the transition can be made
   */
  fun canTake(editor: VimEditor, index: Int) : Boolean {
    return matcher.matches(editor, index)
  }

  /**
   * Determines how many characters are consumed by the transition
   */
  fun consumes() : Int {
    return if (matcher.isEpsilon()) 0 else 1
  }
}