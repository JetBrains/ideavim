/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper
import java.lang.Character.UnicodeBlock

/**
 * This helper class is used when working with various character level operations
 */
object CharacterHelper {
  const val CASE_TOGGLE = '~'
  const val CASE_UPPER = 'u'
  const val CASE_LOWER = 'l'

  /**
   * This returns the type of the supplied character. The logic is as follows:<br></br>
   * If the character is whitespace, `WHITESPACE` is returned.<br></br>
   * If the punctuation is being skipped or the character is a letter, digit, or underscore, `KEYWORD`
   * is returned.<br></br>
   * Otherwise `PUNCTUATION` is returned.
   *
   * @param ch                   The character to analyze
   * @param punctuationAsLetters True if punctuation is to be ignored, false if not
   * @return The type of the character
   */
  @JvmStatic
  fun charType(ch: Char, punctuationAsLetters: Boolean): CharacterType {
    val block = UnicodeBlock.of(ch)
    return if (Character.isWhitespace(ch)) {
      CharacterType.WHITESPACE
    } else if (block === UnicodeBlock.HIRAGANA) {
      CharacterType.HIRAGANA
    } else if (block === UnicodeBlock.KATAKANA) {
      CharacterType.KATAKANA
    } else if (isHalfWidthKatakanaLetter(ch)) {
      CharacterType.HALF_WIDTH_KATAKANA
    } else if (block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
      CharacterType.CJK_UNIFIED_IDEOGRAPHS
    } else if (punctuationAsLetters || KeywordOptionHelper.isKeyword(ch)) {
      CharacterType.KEYWORD
    } else {
      CharacterType.PUNCTUATION
    }
  }

  @JvmStatic
  fun isInvisibleControlCharacter(ch: Char): Boolean {
    val type = Character.getType(ch).toByte()
    return type == Character.CONTROL || type == Character.FORMAT || type == Character.PRIVATE_USE ||
      type == Character.SURROGATE || type == Character.UNASSIGNED
  }

  @JvmStatic
  fun isZeroWidthCharacter(ch: Char) = ch == '\ufeff' || ch == '\u200b' || ch == '\u200c' || ch == '\u200d'

  private fun isHalfWidthKatakanaLetter(ch: Char): Boolean = ch in '\uFF66'..'\uFF9F'

  /**
   * Changes the case of the supplied character based on the supplied change type
   *
   * @param ch   The character to change
   * @param type One of `CASE_TOGGLE`, `CASE_UPPER`, or `CASE_LOWER`
   * @return The character with changed case or the original if not a letter
   */
  @JvmStatic
  fun changeCase(ch: Char, type: Char): Char = when (type) {
    CASE_TOGGLE -> when {
      Character.isLowerCase(ch) -> Character.toUpperCase(ch)
      Character.isUpperCase(ch) -> Character.toLowerCase(ch)
      else -> ch
    }
    CASE_LOWER -> Character.toLowerCase(ch)
    CASE_UPPER -> Character.toUpperCase(ch)
    else -> ch
  }

  enum class CharacterType {
    KEYWORD, HIRAGANA, KATAKANA, HALF_WIDTH_KATAKANA, CJK_UNIFIED_IDEOGRAPHS, PUNCTUATION, WHITESPACE
  }
}
