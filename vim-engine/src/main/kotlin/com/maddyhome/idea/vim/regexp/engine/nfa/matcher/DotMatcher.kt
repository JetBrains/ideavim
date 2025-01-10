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
 * Matcher that matches with any character
 */
internal class DotMatcher(private val includeNewLine: Boolean) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (includeNewLine)
      if (index < editor.text().length) MatcherResult.Success(1)
      else MatcherResult.Failure
    else
      if (index < editor.text().length && editor.text()[index] != '\n') MatcherResult.Success(1)
      else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return false
  }
}