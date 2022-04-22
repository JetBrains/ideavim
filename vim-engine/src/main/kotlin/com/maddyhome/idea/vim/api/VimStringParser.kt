package com.maddyhome.idea.vim.api

import javax.swing.KeyStroke

interface VimStringParser {
  val plugKeyStroke: KeyStroke
  fun parseKeys(vararg strings: String): List<KeyStroke>
  fun stringToKeys(string: String): List<KeyStroke>
  fun toKeyNotation(keyStroke: KeyStroke): String
  fun toKeyNotation(keyStrokes: List<KeyStroke>): String
  fun parseKeysSet(vararg keys: String): Set<List<KeyStroke>>
  fun toKeyCodedString(keys: List<KeyStroke>): String
}
