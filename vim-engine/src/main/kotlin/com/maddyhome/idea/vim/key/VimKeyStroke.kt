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
      return VimKeyStroke(c, VK_UNDEFINED, NO_MODIFIERS)
    }
    fun getKeyStroke(keyChar: Char, modifiers: Int): VimKeyStroke {
      return VimKeyStroke(keyChar, VK_UNDEFINED, modifiers)
    }

    fun getKeyStroke(keycode: Int, modifiers: Int): VimKeyStroke {
      return VimKeyStroke(CHAR_UNDEFINED, keycode, modifiers)
    }
  }
}

const val VK_UNDEFINED = 0x0
const val CHAR_UNDEFINED = 0xFFFF.toChar()
const val NO_MODIFIERS = 0x0
