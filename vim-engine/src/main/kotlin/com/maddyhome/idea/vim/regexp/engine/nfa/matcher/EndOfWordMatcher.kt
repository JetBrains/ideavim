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
 * Matcher used to check if index is at the end of a word.
 */
internal class EndOfWordMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>
  ): MatcherResult {
    if (index > editor.text().length || index == 0) return MatcherResult.Failure

    val prevChar = editor.text()[index - 1]

    /**
     * The current index is the end of a word if the previous one
     * is a keyword character, and the current one isn't.
     */
    return if (
      (prevChar.isLetterOrDigit() || prevChar == '_') &&
      (index == editor.text().length || !(editor.text()[index].isLetterOrDigit() || editor.text()[index] == '_'))
    ) MatcherResult.Success(0)
    else MatcherResult.Failure
  }
}