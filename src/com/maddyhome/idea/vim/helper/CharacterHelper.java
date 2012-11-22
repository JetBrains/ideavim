package com.maddyhome.idea.vim.helper;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.util.Arrays;

/**
 * This helper class is used when working with various character level operations
 */
public class CharacterHelper {
  public static final char CASE_TOGGLE = '~';
  public static final char CASE_UPPER = 'u';
  public static final char CASE_LOWER = 'l';

  public static final int TYPE_CHAR = 1;
  public static final int TYPE_PUNC = 2;
  public static final int TYPE_SPACE = 3;
  public static final int TYPE_KANA = 4;
  public static final int TYPE_WIDE_ALPHANUM = 5;
  public static final int TYPE_WIDE_PUNC = 6;
  public static final int TYPE_WIDE_HIRAGANA = 7;
  public static final int TYPE_WIDE_KATAKANA = 8;
  public static final int TYPE_OTHER = 9;

  private static char[] ALL_HALF_SYMBOL =
    "\u3001\u3002\uff0c\uff0e\uff1a\uff1b\uff1f\uff01\uff40\uff3e\uff3f\uff0f\uff5e\uff5c\u2018\u2019\u201c\u201d\uff08\uff09\uff3b\uff3d\uff5b\uff5d\uff0b\uff0d\uff1d\uff1c\uff1e\uffe5\uff04\uff05\uff03\uff06\uff0a\uff20"
      .toCharArray();
  private static char[] ALL_WIDE_SYMBOL = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".toCharArray();
  private static char[] ALL_HALFKANA =
    "\uff61\uff62\uff63\uff64\uff65\uff66\uff67\uff68\uff69\uff6a\uff6b\uff6c\uff6d\uff6e\uff6f\uff70\uff71\uff72\uff73\uff74\uff75\uff76\uff77\uff78\uff79\uff7a\uff7b\uff7c\uff7d\uff7e\uff7f\uff80\uff81\uff82\uff83\uff84\uff85\uff86\uff87\uff88\uff89\uff8a\uff8b\uff8c\uff8d\uff8e\uff8f\uff90\uff91\uff92\uff93\uff94\uff95\uff96\uff97\uff98\uff99\uff9a\uff9b\uff9c\uff9d\uff9e\uff9f"
      .toCharArray();

  static {
    Arrays.sort(ALL_HALF_SYMBOL);
    Arrays.sort(ALL_WIDE_SYMBOL);
    Arrays.sort(ALL_HALFKANA);
  }

  /**
   * This returns the type of the supplied character. The logic is as follows:<br>
   * If the character is whitespace, <code>TYPE_SPACE</code> is returned.<br>
   * If the punctuation is being skipped or the character is a letter, digit, or underscore, <code>TYPE_CHAR</code>
   * is returned.<br>
   * Otherwise <code>TYPE_PUNC</code> is returned.
   *
   * @param ch                The character to analyze
   * @param punctuationAsChar True if punctuation is to be ignored, false if not
   * @return The type of the character
   */
  public static int charType(char ch, boolean punctuationAsChar) {
    if (Character.isWhitespace(ch)) {
      return TYPE_SPACE;
    }
    else if (punctuationAsChar || isHalfAlphaNum(ch) || ch == '_') {
      return TYPE_CHAR;
    }
    else if (isHalfSymbol(ch)) {
      return TYPE_PUNC;
    }
    else if (isHalfKana(ch)) {
      return TYPE_KANA;
    }
    else if (isWideAlphaNum(ch)) {
      return TYPE_WIDE_ALPHANUM;
    }
    else if (isWideSymbol(ch)) {
      return TYPE_WIDE_PUNC;
    }
    else if (Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.HIRAGANA) {
      return TYPE_WIDE_HIRAGANA;
    }
    else if (Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.KATAKANA) {
      return TYPE_WIDE_KATAKANA;
    }
    else {
      return TYPE_OTHER;
    }
  }

  private static boolean isHalfAlphaNum(char ch) {
    return (0x30 <= ch && ch <= 0x39) || (0x41 <= ch && ch <= 0x5A) || (0x61 <= ch && ch <= 0x7A);
  }

  private static boolean isWideAlphaNum(char ch) {
    return (0xFF10 <= ch && ch <= 0xFF19) || (0xFF21 <= ch && ch <= 0xFF3A) || (0xFF41 <= ch && ch <= 0xFF5A);
  }

  private static boolean isHalfSymbol(char ch) {
    return Arrays.binarySearch(ALL_HALF_SYMBOL, ch) > 0;
  }

  private static boolean isWideSymbol(char ch) {
    return Arrays.binarySearch(ALL_WIDE_SYMBOL, ch) > 0;
  }

  private static boolean isHalfKana(char ch) {
    return Arrays.binarySearch(ALL_HALFKANA, ch) > 0;
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
