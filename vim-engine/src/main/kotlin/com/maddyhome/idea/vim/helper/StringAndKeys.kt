/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

fun KeyStroke.isCloseKeyStroke(): Boolean {
  return keyCode == KeyEvent.VK_ESCAPE ||
    keyChar.code == KeyEvent.VK_ESCAPE ||
    keyCode == KeyEvent.VK_C && modifiers and InputEvent.CTRL_DOWN_MASK != 0 ||
    keyCode == '['.code && modifiers and InputEvent.CTRL_DOWN_MASK != 0
}
