package com.maddyhome.idea.vim.api

import javax.swing.KeyStroke

interface VimStringParser {
  val plugKeyStroke: KeyStroke
  fun parseKeys(vararg strings: String): List<KeyStroke>
  fun isCloseKeyStroke(key: KeyStroke): Boolean
}
