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
import com.maddyhome.idea.vim.state.mode.inVisualMode

/**
 * Matcher used to check if index is inside the visual area.
 */
internal class VisualAreaMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups:
    VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    val newPossibleCursors = if (editor.inVisualMode) {
      possibleCursors.filter { it.hasSelection() && index >= it.selectionStart && index < it.selectionEnd }
    }
    // IdeaVim exits visual mode before command processing (e.g. substitute), so we work with lastSelectionInfo
    else {
      possibleCursors.filter { it.lastSelectionInfo.isSelected(index, editor) }
    }

    return if (newPossibleCursors.isNotEmpty()) {
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
