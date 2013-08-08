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

import org.apache.commons.codec.binary.Base64;
import org.jdom.CDATA;
import org.jdom.Content;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class StringHelper {
  private StringHelper() {}

  @NotNull
  public static String pad(@NotNull String text, int len, char ch) {
    int l = text.length();
    StringBuffer res = new StringBuffer(text);
    for (int i = l; i < len; i++) {
      res.insert(0, ch);
    }

    return res.toString();
  }

  @NotNull
  public static String escape(@NotNull String s) {
    return escape(stringToKeys(s));
  }

  @NotNull
  public static String escape(@NotNull List<KeyStroke> keys) {
    final StringBuffer res = new StringBuffer();
    for (KeyStroke key : keys) {
      final char c = key.getKeyChar();
      final int modifiers = key.getModifiers();
      final int code = key.getKeyCode();
      if (c < ' ') {
        res.append('^').append((char)(c + 'A' - 1));
      }
      else if (c == '\n') {
        res.append("^J");
      }
      else if (c == '\t') {
        res.append("^I");
      }
      else if (c == '\u0000') {
        res.append("^@");
      }
      else if ((modifiers & KeyEvent.CTRL_DOWN_MASK) != 0) {
        final char[] chars = Character.toChars(code);
        if (chars.length == 1) {
          res.append("^");
          res.append(chars);
        }
      }
      else {
        res.append(c);
      }
    }
    return res.toString();
  }

  @NotNull
  @Deprecated
  private static String unentities(@NotNull String text) {
    StringBuffer res = new StringBuffer(text.length());

    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      switch (ch) {
        case '&':
          int semi = text.indexOf(';', i);
          if (semi > i) {
            char newch = ch;
            String entity = text.substring(i, semi + 1);
            if (entity.equals("&#32;")) {
              newch = ' ';
            }
            else if (entity.equals("&#33;")) {
              newch = '!';
              i = semi;
            }
            else if (entity.equals("&#91;")) {
              newch = '[';
              i = semi;
            }
            else if (entity.equals("&#93;")) {
              newch = ']';
              i = semi;
            }
            else if (entity.equals("&amp;")) {
              newch = '&';
              i = semi;
            }
            else if (entity.equals("&#9;")) {
              newch = '\t';
            }
            else if (entity.equals("&#10;")) {
              newch = '\n';
            }
            else if (entity.equals("&#13;")) {
              newch = '\r';
            }
            if (newch != ch) {
              ch = newch;
              i = semi;
            }
          }
          res.append(ch);
          break;
        default:
          res.append(ch);
      }
    }

    return res.toString();
  }

  @NotNull
  public static List<KeyStroke> stringToKeys(@NotNull String s) {
    final List<KeyStroke> res = new ArrayList<KeyStroke>();
    for (int i = 0; i < s.length(); i++) {
      res.add(KeyStroke.getKeyStroke(s.charAt(i)));
    }
    return res;
  }

  public static boolean containsUpperCase(@NotNull String text) {
    for (int i = 0; i < text.length(); i++) {
      if (Character.isUpperCase(text.charAt(i)) && (i == 0 || text.charAt(i - 1) == '\\')) {
        return true;
      }
    }

    return false;
  }

  /**
   * Set the text of an XML element, safely encode it if needed.
   */
  @NotNull
  public static Element setSafeXmlText(@NotNull Element element, @NotNull String text) {
    final Character first = firstCharacter(text);
    final Character last = lastCharacter(text);
    if (!StringHelper.isXmlCharacterData(text) ||
        first != null && Character.isWhitespace(first) ||
        last != null && Character.isWhitespace(last)) {
      element.setAttribute("encoding", "base64");
      final String encoded = new String(Base64.encodeBase64(text.getBytes()));
      element.setText(encoded);
    }
    else {
      element.setText(text);
    }
    return element;
  }

  @Nullable
  private static Character lastCharacter(@NotNull String text) {
    return text.length() > 0 ? text.charAt(text.length() - 1) : null;
  }

  @Nullable
  private static Character firstCharacter(@NotNull String text) {
    return text.length() > 0 ? text.charAt(0) : null;
  }

  /**
   * Get the (potentially safely encoded) text of an XML element.
   */
  @Nullable
  public static String getSafeXmlText(@NotNull Element element) {
    final String text = element.getText();
    //noinspection unchecked
    final List<Content> contentItems = element.getContent();
    for (Content content : contentItems) {
      if (content instanceof CDATA) {
        // TODO: Remove compatibility with the IdeaVim <= 0.24 settings style in the next versions
        return unentities(text);
      }
    }
    final String encoding = element.getAttributeValue("encoding");
    if (encoding == null) {
      return text;
    }
    else if (encoding.equals("base64")) {
      return new String(Base64.decodeBase64(text.getBytes()));
    }
    return null;
  }

  /**
   * Check if the text matches the CharData production from the XML grammar.
   *
   * This check is more restricted than CharData as it completely forbids '>'.
   */
  public static boolean isXmlCharacterData(@NotNull String text) {
    for (char c : text.toCharArray()) {
      if (!isXmlChar(c) || c == '<' || c == '>' || c == '&') {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if a char matches the Char production from the XML grammar.
   *
   * Characters beyond the Basic Multilingual Plane are not supported.
   */
  private static boolean isXmlChar(char c) {
    return '\u0001' <= c && c <= '\uD7FF' || '\uE000' <= c && c <= '\uFFFD';
  }
}
