/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

import com.maddyhome.idea.vim.api.VimEditor

internal class PredicateMatcher(val predicate: (Char) -> Boolean) : Matcher {
  override fun matches(editor: VimEditor, index: Int): Boolean {
    return index < editor.text().length && predicate(editor.text()[index])
  }

  override fun isEpsilon(): Boolean {
    return false
  }
}