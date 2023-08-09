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

internal class CollectionMatcher(
  private val chars: List<Char> = emptyList(),
  private val ranges: List<CollectionRange> = emptyList(),
  private val isNegated: Boolean = false,
  private val includesEOL: Boolean = false
) : Matcher {
  override fun matches(editor: VimEditor, index: Int, groups: VimMatchGroupCollection): MatcherResult {
    if (index >= editor.text().length) return MatcherResult.Failure

    if (!includesEOL && editor.text()[index] == '\n') return MatcherResult.Failure
    if (includesEOL && editor.text()[index] == '\n') return MatcherResult.Success(1)

    val char = editor.text()[index]
    val result = (chars.contains(char) || ranges.any { it.inRange(char) }) == !isNegated
    return if (result) MatcherResult.Success(1)
    else MatcherResult.Failure
  }
}

internal data class CollectionRange(val start: Char, val end: Char) {
  internal fun inRange(char: Char) : Boolean {
    return char.code in start.code..end.code
  }
}