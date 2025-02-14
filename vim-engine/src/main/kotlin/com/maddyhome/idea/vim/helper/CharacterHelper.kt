/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper
import java.lang.Character.UnicodeBlock

/**
 * This helper class is used when working with various character level operations
 */
object CharacterHelper {
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
  fun charType(editor: VimEditor, ch: Char, punctuationAsLetters: Boolean): CharacterType {
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
    } else if (punctuationAsLetters || KeywordOptionHelper.isKeyword(editor, ch)) {
      CharacterType.KEYWORD
    } else {
      CharacterType.PUNCTUATION
    }
  }

  @JvmStatic
  fun isWhitespace(editor: VimEditor, ch: Char, isBig: Boolean): Boolean =
    charType(editor, ch, isBig) == CharacterType.WHITESPACE

  @JvmStatic
  fun isInvisibleControlCharacter(ch: Char): Boolean {
    val type = Character.getType(ch).toByte()
    return type == Character.CONTROL || type == Character.FORMAT || type == Character.PRIVATE_USE ||
      type == Character.SURROGATE || type == Character.UNASSIGNED
  }

  @JvmStatic
  fun isZeroWidthCharacter(ch: Char): Boolean = ch == '\ufeff' || ch == '\u200b' || ch == '\u200c' || ch == '\u200d'

  private fun isHalfWidthKatakanaLetter(ch: Char): Boolean = ch in '\uFF66'..'\uFF9F'

  enum class CharacterType {
    KEYWORD, HIRAGANA, KATAKANA, HALF_WIDTH_KATAKANA, CJK_UNIFIED_IDEOGRAPHS, PUNCTUATION, WHITESPACE
  }
}
