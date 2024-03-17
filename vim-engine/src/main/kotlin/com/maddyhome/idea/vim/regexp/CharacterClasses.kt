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
public object CharacterClasses {
  public val CLASS_NAMES: @NonNls Array<String> = arrayOf(
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
  public const val RI_DIGIT: Int = 0x01
  public const val RI_HEX: Int = 0x02
  public const val RI_OCTAL: Int = 0x04
  public const val RI_WORD: Int = 0x08
  public const val RI_HEAD: Int = 0x10
  public const val RI_ALPHA: Int = 0x20
  public const val RI_LOWER: Int = 0x40
  public const val RI_UPPER: Int = 0x80
  public const val RI_WHITE: Int = 0x100
  public const val CLASS_ALNUM: Int = 0
  public const val CLASS_ALPHA: Int = 1
  public const val CLASS_BLANK: Int = 2
  public const val CLASS_CNTRL: Int = 3
  public const val CLASS_DIGIT: Int = 4
  public const val CLASS_GRAPH: Int = 5
  public const val CLASS_LOWER: Int = 6
  public const val CLASS_PRINT: Int = 7
  public const val CLASS_PUNCT: Int = 8
  public const val CLASS_SPACE: Int = 9
  public const val CLASS_UPPER: Int = 10
  public const val CLASS_XDIGIT: Int = 11
  public const val CLASS_TAB: Int = 12
  public const val CLASS_RETURN: Int = 13
  public const val CLASS_BACKSPACE: Int = 14
  public const val CLASS_ESCAPE: Int = 15
  public const val CLASS_NONE: Int = 99
  public fun isMask(ch: Char, mask: Int, test: Int): Boolean {
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

  public fun isHex(ch: Char): Boolean {
    return (ch.digitToIntOrNull(16) ?: -1) != -1
  }

  public fun isOctal(ch: Char): Boolean {
    return (ch.digitToIntOrNull(8) ?: -1) != -1
  }

  public fun isWord(ch: Char): Boolean {
    return Character.isLetterOrDigit(ch) || ch == '_'
  }

  public fun isHead(ch: Char): Boolean {
    return Character.isLetter(ch) || ch == '_'
  }

  @JvmStatic
  public fun isAlpha(ch: Char): Boolean {
    return Character.isLetter(ch)
  }

  public fun isLower(ch: Char): Boolean {
    return Character.isLowerCase(ch)
  }

  public fun isUpper(ch: Char): Boolean {
    return Character.isUpperCase(ch)
  }

  public fun isWhite(ch: Char): Boolean {
    return Character.isWhitespace(ch)
  }

  public fun isGraph(ch: Char): Boolean {
    return ch >= '!' && ch <= '~'
  }

  public fun isPrint(ch: Char): Boolean {
    return ch >= ' ' && ch <= '~' || ch.code > 0xff
  }

  public fun isPunct(ch: Char): Boolean {
    return ch in '!'..'/' ||
      ch in ':'..'@' ||
      ch in '['..'`' ||
      ch in '{'..'~'
  }

  public fun isFile(ch: Char): Boolean {
    return isWord(ch) || "/.-+,#$%~=".indexOf(ch) != -1
  }
}
