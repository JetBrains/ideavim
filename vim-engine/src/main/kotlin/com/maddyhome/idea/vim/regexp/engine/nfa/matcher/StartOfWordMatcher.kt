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
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper
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
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    if (index >= editor.text().length) return MatcherResult.Failure

    val isKeywordAtIndex = KeywordOptionHelper.isKeyword(editor, editor.text()[index])
    val isKeywordBeforeIndex =
      editor.text().getOrNull(index - 1)?.let { KeywordOptionHelper.isKeyword(editor, it) } ?: false

    return if (!isKeywordBeforeIndex && isKeywordAtIndex) MatcherResult.Success(0) else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}