/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.regexp;

public final class CharacterClasses {
  private CharacterClasses() {
  }

  public static final String[] CLASS_NAMES = new String[]{
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
    "escape:]"
  };

  public static final int RI_DIGIT = 0x01;
  public static final int RI_HEX = 0x02;
  public static final int RI_OCTAL = 0x04;
  public static final int RI_WORD = 0x08;
  public static final int RI_HEAD = 0x10;
  public static final int RI_ALPHA = 0x20;
  public static final int RI_LOWER = 0x40;
  public static final int RI_UPPER = 0x80;
  public static final int RI_WHITE = 0x100;

  public static final int CLASS_ALNUM = 0;
  public static final int CLASS_ALPHA = 1;
  public static final int CLASS_BLANK = 2;
  public static final int CLASS_CNTRL = 3;
  public static final int CLASS_DIGIT = 4;
  public static final int CLASS_GRAPH = 5;
  public static final int CLASS_LOWER = 6;
  public static final int CLASS_PRINT = 7;
  public static final int CLASS_PUNCT = 8;
  public static final int CLASS_SPACE = 9;
  public static final int CLASS_UPPER = 10;
  public static final int CLASS_XDIGIT = 11;
  public static final int CLASS_TAB = 12;
  public static final int CLASS_RETURN = 13;
  public static final int CLASS_BACKSPACE = 14;
  public static final int CLASS_ESCAPE = 15;
  public static final int CLASS_NONE = 99;

  public static boolean isMask(char ch, int mask, int test) {
    boolean res = false;
    switch (mask) {
      case RI_DIGIT:
        res = Character.isDigit(ch);
        break;
      case RI_HEX:
        res = isHex(ch);
        break;
      case RI_OCTAL:
        res = isOctal(ch);
        break;
      case RI_WORD:
        res = isWord(ch);
        break;
      case RI_HEAD:
        res = isHead(ch);
        break;
      case RI_ALPHA:
        res = isAlpha(ch);
        break;
      case RI_LOWER:
        res = isLower(ch);
        break;
      case RI_UPPER:
        res = isUpper(ch);
        break;
      case RI_WHITE:
        res = isWhite(ch);
        break;
    }

    return (res == (test > 0));
  }

  public static boolean isHex(char ch) {
    return Character.digit(ch, 16) != -1;
  }

  public static boolean isOctal(char ch) {
    return Character.digit(ch, 8) != -1;
  }

  public static boolean isWord(char ch) {
    return Character.isLetterOrDigit(ch) || ch == '_';
  }

  public static boolean isHead(char ch) {
    return Character.isLetter(ch) || ch == '_';
  }

  public static boolean isAlpha(char ch) {
    return Character.isLetter(ch);
  }

  public static boolean isLower(char ch) {
    return Character.isLowerCase(ch);
  }

  public static boolean isUpper(char ch) {
    return Character.isUpperCase(ch);
  }

  public static boolean isWhite(char ch) {
    return Character.isWhitespace(ch);
  }

  public static boolean isGraph(char ch) {
    return (ch >= '!' && ch <= '~');
  }

  public static boolean isPrint(char ch) {
    return ((ch >= ' ' && ch <= '~') || ch > 0xff);
  }

  public static boolean isPunct(char ch) {
    return ((ch >= '!' && ch <= '/') ||
            (ch >= ':' && ch <= '@') ||
            (ch >= '[' && ch <= '`') ||
            (ch >= '{' && ch <= '~'));
  }

  public static boolean isFile(char ch) {
    return (isWord(ch) || "/.-+,#$%~=".indexOf(ch) != -1);
  }
}
