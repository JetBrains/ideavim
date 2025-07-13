/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.key.VimKeyStroke
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

val KeyStroke.vimKeyStroke: VimKeyStroke
  get() {
    return VimKeyStroke(keyChar,  keyCode, modifiers)
  }
val VimKeyStroke.keyStroke: KeyStroke
  get() {
    return if (KeyEvent.VK_UNDEFINED == keyCode) {
      KeyStroke.getKeyStroke(keyChar, modifiers)
    } else {
      KeyStroke.getKeyStroke(keyCode, modifiers)
    }
  }
