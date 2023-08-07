/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

import com.maddyhome.idea.vim.api.VimEditor

/**
 * Matcher used to match against single characters
 */
internal class CharacterMatcher(val char: Char) : Matcher {
  override fun matches(editor: VimEditor, index: Int): Boolean {
    return index < editor.text().length && editor.text()[index] == char
  }

  override fun isEpsilon(): Boolean {
    return false
  }

}