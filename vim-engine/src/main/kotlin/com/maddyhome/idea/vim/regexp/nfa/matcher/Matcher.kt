/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection
import com.maddyhome.idea.vim.regexp.match.VimMatchResult

/**
 * A matcher is used to decide if a transition can be taken,
 * depending on what character is next in the input as well
 * as other information contained in the editor or in a NFA state
 */
internal interface Matcher {
  /**
   * Determines whether the matcher should match.
   *
   * @param editor The editor in its current state
   * @param index  The current index in the text of the editor
   * @param groups The groups captured so far
   */
  fun matches(editor: VimEditor, index : Int, groups: VimMatchGroupCollection): MatcherResult
}