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
