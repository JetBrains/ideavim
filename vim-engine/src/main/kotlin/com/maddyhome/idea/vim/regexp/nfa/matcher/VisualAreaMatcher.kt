/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection

/**
 * Matcher used to check if index is inside the visual area.
 */
internal class VisualAreaMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int, groups:
    VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>
  ): MatcherResult {
    return if (possibleCursors.any { index >= it.selectionStart && index < it.selectionEnd }) {
      val newPossibleCursors = possibleCursors.filter { index >= it.selectionStart && index < it.selectionEnd }
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    } else {
      MatcherResult.Failure
    }
  }
}