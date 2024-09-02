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

object EngineStringHelper {
  fun toPrintableCharacters(keys: List<KeyStroke>): String {
    if (keys.isEmpty()) {
      return ""
    }
    val builder = StringBuilder()
    for (key in keys) {
      builder.append(toPrintableCharacter(key))
    }
    return builder.toString()
  }

  /**
   * Convert a KeyStroke into the character it represents and return a printable version of the character.
   *
   *
   * See :help 'isprint'
   *
   * @param key The KeyStroke to represent
   * @return A printable String of the character represented by the KeyStroke
   */
  @JvmStatic
  fun toPrintableCharacter(key: KeyStroke): String {
    // TODO: Look at 'isprint', 'display' and 'encoding' settings
    var c = key.keyChar
    if (c == KeyEvent.CHAR_UNDEFINED && key.modifiers == 0) {
      c = key.keyCode.toChar()
    } else if (c == KeyEvent.CHAR_UNDEFINED && key.modifiers and InputEvent.CTRL_DOWN_MASK != 0) {
      c = (key.keyCode - 'A'.code + 1).toChar()
    }
    return toPrintableCharacter(c)
  }

  fun toPrintableCharacter(c: Char): String {
    if (c.code <= 31) {
      return "^" + (c.code + 'A'.code - 1).toChar()
    } else if (c.code == 127) {
      return "^" + (c.code - 'A'.code + 1).toChar()
      // Vim doesn't use these representations unless :set encoding=latin1. Technically, we could use them if the
      // encoding of the buffer for the mark, jump or :ascii char is. But what encoding would we use for registers?
      // Since we support Unicode, just treat everything as Unicode.
//    } else if (c >= 128 && c <= 159) {
//      return "~" + (char) (c - 'A' + 1);
//    } else if (c >= 160 && c <= 254) {
//      return "|" + (char)(c - (('A' - 1) * 2));
//    } else if (c == 255) {
//      return "~" + (char)(c - (('A' - 1) * 3));
    } else if (CharacterHelper.isInvisibleControlCharacter(c) || CharacterHelper.isZeroWidthCharacter(c)) {
      if (c.code > 0xff) {
        return String.format("<%04x>", c.code)
      }
      return String.format("<%02x>", c.code)
    }
    return c.toString()
  }
}

// https://stackoverflow.com/a/14652763/3124227
fun String.removeAsciiColorCodes(): String {
  return this.replace("\u001B\\[[;\\d]*m".toRegex(), "")
}

internal fun String.indexOfOrNull(char: Char, startIndex: Int = 0): Int? {
  val index = this.indexOf(char, startIndex)
  return if (index < 0) null else index
}

internal fun String.lastIndexOfOrNull(char: Char, startIndex: Int = 0, endIndex: Int = length): Int? {
  if (startIndex < 0 || endIndex > this.length) return null
  var i = endIndex - 1
  while (i >= startIndex) {
    if (this[i] == char) return i
    --i
  }
  return null
}
