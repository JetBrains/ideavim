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
import com.maddyhome.idea.vim.regexp.match.VimMatchResult

/**
 * Matcher used to match against single characters
 */
internal class CharacterMatcher(val char: Char) : Matcher {
  override fun matches(editor: VimEditor, index: Int, groups: VimMatchGroupCollection): MatcherResult {
    return if (index < editor.text().length && editor.text()[index] == char) MatcherResult.Success(1)
    else MatcherResult.Failure
  }
}