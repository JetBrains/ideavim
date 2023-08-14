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

internal class StartOfLineMatcher : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
  ): MatcherResult {
    return if (index == 0 || editor.text()[index - 1] == '\n') MatcherResult.Success(0)
    else MatcherResult.Failure
  }
}