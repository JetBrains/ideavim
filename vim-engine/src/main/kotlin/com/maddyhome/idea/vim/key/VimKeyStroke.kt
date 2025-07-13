/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

data class VimKeyStroke(val keyChar: Char, val keyCode: Int, val modifiers: Int) {
  companion object {
    fun getKeyStroke(c: Char): VimKeyStroke {
      TODO("zeauberg")
    }

    fun getKeyStroke(keycode: Int, modifiers: Int): VimKeyStroke {
      TODO("zeauberg")
    }
  }
}
