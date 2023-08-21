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

/**
 * Matcher used to check if index is at the start of a word.
 */
internal class StartOfWordMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
  ): MatcherResult {
    if (index >= editor.text().length) return MatcherResult.Failure

    val char = editor.text()[index]

    /**
     * The current index is the start of a word if it is a keyword character,
     * and the previous index isn't.
     */
    return if (
      (char.isLetterOrDigit() || char == '_') &&
      (index == 0 || !(editor.text()[index - 1].isLetterOrDigit() || editor.text()[index - 1] == '_'))
    ) MatcherResult.Success(0)
    else MatcherResult.Failure
  }
}