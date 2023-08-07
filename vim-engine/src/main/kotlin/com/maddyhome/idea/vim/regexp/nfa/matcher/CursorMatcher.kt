/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.nfa.matcher

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.Offset

public class CursorMatcher : Matcher {
  override fun matches(editor: VimEditor, index: Int): Boolean {
    return editor.carets().map { it.offset }.contains(Offset(index))
  }

  override fun isEpsilon(): Boolean {
    return true
  }
}