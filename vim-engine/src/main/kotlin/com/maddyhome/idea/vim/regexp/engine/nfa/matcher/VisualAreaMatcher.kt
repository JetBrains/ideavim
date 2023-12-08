/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.engine.nfa.matcher

import com.maddyhome.idea.vim.api.SelectionInfo
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection
import com.maddyhome.idea.vim.state.mode.Mode
import kotlin.math.max
import kotlin.math.min

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
    if (!injector.processGroup.isCommandProcessing || injector.processGroup.modeBeforeCommandProcessing !is Mode.VISUAL) return MatcherResult.Failure

    val newPossibleCursors = possibleCursors.filter { it.lastSelectionInfo.contains(editor, index) }
    return if (newPossibleCursors.isNotEmpty()) {
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    } else {
      MatcherResult.Failure
    }
  }

  private fun SelectionInfo.contains(editor: VimEditor, offset: Int): Boolean {
    val startOffset = start?.let { editor.bufferPositionToOffset(it) } ?: return false
    val endOffset = end?.let { editor.bufferPositionToOffset(it) } ?: return false
    return offset >= min(startOffset, endOffset) && offset < max(startOffset, endOffset)
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}