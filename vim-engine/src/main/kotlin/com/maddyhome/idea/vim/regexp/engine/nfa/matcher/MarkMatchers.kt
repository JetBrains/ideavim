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

internal abstract class BaseMarkMatcher(val mark: Char) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    val newPossibleCursors = possibleCursors.filter { matchesCondition(index, it) }
    return if (newPossibleCursors.isNotEmpty()) {
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    } else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean = true

  abstract fun matchesCondition(index: Int, caret: VimCaret): Boolean
  protected fun getMarkOffset(caret: VimCaret): Int? = caret.markStorage.getMark(mark)?.offset(caret.editor)
}

internal class AtMarkMatcher(mark: Char) : BaseMarkMatcher(mark) {
  override fun matchesCondition(index: Int, caret: VimCaret): Boolean = index == getMarkOffset(caret)
}

internal class BeforeMarkMatcher(mark: Char) : BaseMarkMatcher(mark) {
  override fun matchesCondition(index: Int, caret: VimCaret): Boolean =
    getMarkOffset(caret)?.let { index < it } ?: false
}

internal class AfterMarkMatcher(mark: Char) : BaseMarkMatcher(mark) {
  override fun matchesCondition(index: Int, caret: VimCaret): Boolean =
    getMarkOffset(caret)?.let { index > it } ?: false
}