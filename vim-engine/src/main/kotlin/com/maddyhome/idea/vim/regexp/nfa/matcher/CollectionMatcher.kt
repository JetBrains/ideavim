/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

import com.maddyhome.idea.vim.api.VimEditor

internal class CollectionMatcher(
  private val chars: List<Char> = emptyList(),
  private val ranges: List<CollectionRange> = emptyList(),
  private val isNegated: Boolean = false
) : Matcher {
  override fun matches(editor: VimEditor, index: Int): Boolean {
    if (index >= editor.text().length) return false

    val char = editor.text()[index]
    val result = chars.contains(char) || ranges.any { it.inRange(char) }
    return if (isNegated) !result else result
  }

  override fun isEpsilon(): Boolean {
    return false
  }
}

internal data class CollectionRange(val start: Int, val end: Int) {
  internal fun inRange(char: Char) : Boolean {
    return char.code in start..end
  }
}