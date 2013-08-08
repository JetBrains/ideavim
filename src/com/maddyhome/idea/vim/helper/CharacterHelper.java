/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper;

import org.jetbrains.annotations.NotNull;

/**
 * This helper class is used when working with various character level operations
 */
public class CharacterHelper {

  public static enum CharacterType {
    LETTER_OR_DIGIT,
    HIRAGANA,
    KATAKANA,
    HALF_WIDTH_KATAKANA,
    PUNCTUATION,
    WHITESPACE;
  }

  public static final char CASE_TOGGLE = '~';
  public static final char CASE_UPPER = 'u';
  public static final char CASE_LOWER = 'l';

  /**
   * This returns the type of the supplied character. The logic is as follows:<br>
   * If the character is whitespace, <code>WHITESPACE</code> is returned.<br>
   * If the punctuation is being skipped or the character is a letter, digit, or underscore, <code>LETTER_OR_DIGIT</code>
   * is returned.<br>
   * Otherwise <code>PUNCTUATION</code> is returned.
   *
   * @param ch                   The character to analyze
   * @param punctuationAsLetters True if punctuation is to be ignored, false if not
   * @return The type of the character
   */
  @NotNull
  public static CharacterType charType(char ch, boolean punctuationAsLetters) {
    final Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
    if (Character.isWhitespace(ch)) {
      return CharacterType.WHITESPACE;
    }
    else if (block == Character.UnicodeBlock.HIRAGANA) {
      return CharacterType.HIRAGANA;
    }
    else if (block == Character.UnicodeBlock.KATAKANA) {
      return CharacterType.KATAKANA;
    }
    else if (isHalfWidthKatakanaLetter(ch)) {
      return CharacterType.HALF_WIDTH_KATAKANA;
    }
    else if (punctuationAsLetters || Character.isLetterOrDigit(ch) || ch == '_') {
      return CharacterType.LETTER_OR_DIGIT;
    }
    else {
      return CharacterType.PUNCTUATION;
    }
  }

  private static boolean isHalfWidthKatakanaLetter(char ch) {
    return ch >= '\uFF66' && ch <= '\uFF9F';
  }

  /**
   * Changes the case of the supplied character based on the supplied change type
   *
   * @param ch   The character to change
   * @param type One of <code>CASE_TOGGLE</code>, <code>CASE_UPPER</code>, or <code>CASE_LOWER</code>
   * @return The character with changed case or the original if not a letter
   */
  public static char changeCase(char ch, char type) {
    switch (type) {
      case CASE_TOGGLE:
        if (Character.isLowerCase(ch)) {
          ch = Character.toUpperCase(ch);
        }
        else if (Character.isUpperCase(ch)) {
          ch = Character.toLowerCase(ch);
        }
        break;
      case CASE_LOWER:
        ch = Character.toLowerCase(ch);
        break;
      case CASE_UPPER:
        ch = Character.toUpperCase(ch);
        break;
    }

    return ch;
  }
}
