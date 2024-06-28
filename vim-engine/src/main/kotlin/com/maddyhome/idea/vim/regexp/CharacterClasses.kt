/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.regexp

import org.jetbrains.annotations.NonNls

@Deprecated("Remove once old regex engine is removed")
object CharacterClasses {
  val CLASS_NAMES: @NonNls Array<String> = arrayOf(
    "alnum:]",
    "alpha:]",
    "blank:]",
    "cntrl:]",
    "digit:]",
    "graph:]",
    "lower:]",
    "print:]",
    "punct:]",
    "space:]",
    "upper:]",
    "xdigit:]",
    "tab:]",
    "return:]",
    "backspace:]",
    "escape:]",
  )
  const val RI_DIGIT: Int = 0x01
  const val RI_HEX: Int = 0x02
  const val RI_OCTAL: Int = 0x04
  const val RI_WORD: Int = 0x08
  const val RI_HEAD: Int = 0x10
  const val RI_ALPHA: Int = 0x20
  const val RI_LOWER: Int = 0x40
  const val RI_UPPER: Int = 0x80
  const val RI_WHITE: Int = 0x100
  const val CLASS_ALNUM: Int = 0
  const val CLASS_ALPHA: Int = 1
  const val CLASS_BLANK: Int = 2
  const val CLASS_CNTRL: Int = 3
  const val CLASS_DIGIT: Int = 4
  const val CLASS_GRAPH: Int = 5
  const val CLASS_LOWER: Int = 6
  const val CLASS_PRINT: Int = 7
  const val CLASS_PUNCT: Int = 8
  const val CLASS_SPACE: Int = 9
  const val CLASS_UPPER: Int = 10
  const val CLASS_XDIGIT: Int = 11
  const val CLASS_TAB: Int = 12
  const val CLASS_RETURN: Int = 13
  const val CLASS_BACKSPACE: Int = 14
  const val CLASS_ESCAPE: Int = 15
  const val CLASS_NONE: Int = 99
  fun isMask(ch: Char, mask: Int, test: Int): Boolean {
    var res = false
    when (mask) {
      RI_DIGIT -> res = Character.isDigit(ch)
      RI_HEX -> res = isHex(ch)
      RI_OCTAL -> res = isOctal(ch)
      RI_WORD -> res = isWord(ch)
      RI_HEAD -> res = isHead(ch)
      RI_ALPHA -> res = isAlpha(ch)
      RI_LOWER -> res = isLower(ch)
      RI_UPPER -> res = isUpper(ch)
      RI_WHITE -> res = isWhite(ch)
    }
    return res == test > 0
  }

  fun isHex(ch: Char): Boolean {
    return (ch.digitToIntOrNull(16) ?: -1) != -1
  }

  fun isOctal(ch: Char): Boolean {
    return (ch.digitToIntOrNull(8) ?: -1) != -1
  }

  fun isWord(ch: Char): Boolean {
    return Character.isLetterOrDigit(ch) || ch == '_'
  }

  fun isHead(ch: Char): Boolean {
    return Character.isLetter(ch) || ch == '_'
  }

  @JvmStatic
  fun isAlpha(ch: Char): Boolean {
    return Character.isLetter(ch)
  }

  fun isLower(ch: Char): Boolean {
    return Character.isLowerCase(ch)
  }

  fun isUpper(ch: Char): Boolean {
    return Character.isUpperCase(ch)
  }

  fun isWhite(ch: Char): Boolean {
    return Character.isWhitespace(ch)
  }

  fun isGraph(ch: Char): Boolean {
    return ch >= '!' && ch <= '~'
  }

  fun isPrint(ch: Char): Boolean {
    return ch >= ' ' && ch <= '~' || ch.code > 0xff
  }

  fun isPunct(ch: Char): Boolean {
    return ch in '!'..'/' ||
      ch in ':'..'@' ||
      ch in '['..'`' ||
      ch in '{'..'~'
  }

  fun isFile(ch: Char): Boolean {
    return isWord(ch) || "/.-+,#$%~=".indexOf(ch) != -1
  }
}
