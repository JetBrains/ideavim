/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.engine.nfa.matcher

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection

/**
 * Matcher that matches if there is a cursor
 * on the given index
 */
internal class CursorMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups:
    VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (possibleCursors.map { it.offset }.contains(index)) {
      // now the only cursors possible are the ones at this index
      val newPossibleCursors = possibleCursors.filter { it.offset == index }
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    } else {
      MatcherResult.Failure
    }
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}