package com.maddyhome.idea.vim.helper

import javax.swing.KeyStroke

interface VimStringParser {
  val plugKeyStroke: KeyStroke
  fun parseKeys(vararg strings: String): List<KeyStroke>
  fun isCloseKeyStroke(key: KeyStroke): Boolean
}
