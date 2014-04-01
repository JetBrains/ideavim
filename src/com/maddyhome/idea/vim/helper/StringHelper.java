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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.codec.binary.Base64;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.awt.event.KeyEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;

public class StringHelper {
  private static final String CTRL_PREFIX = "c-";
  private static final ImmutableMap<String, Integer> VIM_KEY_NAMES = ImmutableMap.<String, Integer>builder()
    .put("enter", VK_ENTER)
    .put("ins", VK_INSERT)
    .put("insert", VK_INSERT)
    .put("del", VK_DELETE)
    .put("delete", VK_DELETE)
    .put("esc", VK_ESCAPE)
    .put("tab", VK_TAB)
    .put("up", VK_UP)
    .put("down", VK_DOWN)
    .put("left", VK_LEFT)
    .put("right", VK_RIGHT)
    .build();

  private StringHelper() {}

  @NotNull
  public static String pad(@NotNull String text, int len, char ch) {
    int l = text.length();
    StringBuilder res = new StringBuilder(text);
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
    final StringBuilder res = new StringBuilder();
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
      else if ((modifiers & CTRL_DOWN_MASK) != 0) {
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
  public static List<KeyStroke> stringToKeys(@NotNull String s) {
    final List<KeyStroke> res = new ArrayList<KeyStroke>();
    for (int i = 0; i < s.length(); i++) {
      res.add(getKeyStroke(s.charAt(i)));
    }
    return res;
  }

  private static enum KeyParserState {
    INIT,
    ESCAPE,
    SPECIAL,
  }

  /**
   * Parses Vim key notation strings.
   *
   * @see :help <>
   */
  @NotNull
  public static List<KeyStroke> parseKeys(@NotNull String... strings) {
    final List<KeyStroke> result = new ArrayList<KeyStroke>();
    for (String s : strings) {
      KeyParserState state = KeyParserState.INIT;
      StringBuilder specialKeyBuilder = new StringBuilder();
      for (int i = 0; i < s.length(); i++) {
        final char c = s.charAt(i);
        switch (state) {
          case INIT:
            if (c == '\\') {
              state = KeyParserState.ESCAPE;
            }
            else if (c == '<') {
              state = KeyParserState.SPECIAL;
              specialKeyBuilder = new StringBuilder();
            }
            else {
              result.add(getKeyStroke(c));
            }
            break;
          case ESCAPE:
            state = KeyParserState.INIT;
            if (c == '\\' || c == '<') {
              result.add(getKeyStroke(c));
            }
            else {
              result.add(getKeyStroke('\\'));
              result.add(getKeyStroke(c));
            }
            break;
          case SPECIAL:
            if (c == '>') {
              state = KeyParserState.INIT;
              final String specialKeyName = specialKeyBuilder.toString();
              final KeyStroke specialKey = parseSpecialKey(specialKeyName, 0);
              if (specialKey != null) {
                result.add(specialKey);
              }
              else {
                result.add(getKeyStroke('<'));
                result.addAll(stringToKeys(specialKeyName));
                result.add(getKeyStroke('>'));
              }
            }
            else {
              specialKeyBuilder.append(c);
            }
            break;
        }
      }
      if (state == KeyParserState.ESCAPE) {
        result.add(getKeyStroke('\\'));
      }
      else if (state == KeyParserState.SPECIAL) {
        result.add(getKeyStroke('<'));
        result.addAll(stringToKeys(specialKeyBuilder.toString()));
      }
    }
    return result;
  }

  @NotNull
  public static Set<List<KeyStroke>> parseKeysSet(@NotNull String... keyStrings) {
    final ImmutableSet.Builder<List<KeyStroke>> builder = ImmutableSet.builder();
    for (String keyString : keyStrings) {
      builder.add(parseKeys(keyString));
    }
    return builder.build();
  }

  @Nullable
  private static KeyStroke parseSpecialKey(@NotNull String s, int modifiers) {
    final String lower = s.toLowerCase();
    final Integer keyCode = VIM_KEY_NAMES.get(lower);
    if (keyCode != null) {
      return getKeyStroke(keyCode, modifiers);
    }
    else if (lower.startsWith(CTRL_PREFIX)) {
      return parseSpecialKey(s.substring(CTRL_PREFIX.length()), modifiers | CTRL_MASK);
    }
    else if (s.length() == 1) {
      return getKeyStroke(s.charAt(0), modifiers);
    }
    return null;
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
