/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.DigraphScope
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector

class DigraphScopeImpl : DigraphScope {
  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  override fun getCharacter(ch1: Char, ch2: Char): Int {
    return injector.digraphGroup.getCharacterForDigraph(ch1, ch2)
  }

  override fun add(ch1: Char, ch2: Char, codepoint: Int) {
    val args = "$ch1$ch2 $codepoint"
    injector.digraphGroup.parseCommandLine(vimEditor, args)
  }
}
