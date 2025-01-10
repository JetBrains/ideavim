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
 * Matcher used to match a character against a predicate
 *
 * @param predicate The predicate used to check if the character should be accepted
 */
internal class PredicateMatcher(val predicate: (Char) -> Boolean) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups:
    VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (index < editor.text().length && predicate(editor.text()[index])) MatcherResult.Success(1)
    else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return false
  }
}

/**
 * Matcher used to match a character against a predicate
 *
 * @param predicate The predicate used to check if the character should be accepted
 */
internal class EditorAwarePredicateMatcher(val predicate: (VimEditor, Char) -> Boolean) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups:
    VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>,
  ): MatcherResult {
    return if (index < editor.text().length && predicate(editor, editor.text()[index])) MatcherResult.Success(1)
    else MatcherResult.Failure
  }

  override fun isEpsilon(): Boolean {
    return false
  }
}
