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

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.util.text.StringUtil;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment;
import org.apache.commons.codec.binary.Base64;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static java.awt.event.KeyEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;

public class StringHelper {
  private static final String META_PREFIX = "m-";
  private static final String ALT_PREFIX = "a-";
  private static final String CTRL_PREFIX = "c-";
  private static final String SHIFT_PREFIX = "s-";

  /**
   * Fake key for <Plug> mappings
   */
  private static final int VK_PLUG = KeyEvent.CHAR_UNDEFINED - 1;

  private StringHelper() {}

  @Nullable
  private static String toEscapeNotation(@NotNull KeyStroke key) {
    final char c = key.getKeyChar();
    if (isControlCharacter(c)) {
      return "^" + (char)(c + 'A' - 1);
    }
    else if (isControlKeyCode(key)) {
      return "^" + (char)(key.getKeyCode() + 'A' - 1);
    }
    return null;
  }

  @NotNull
  public static List<KeyStroke> stringToKeys(@NotNull String s) {
    final List<KeyStroke> res = new ArrayList<>();
    for (int i = 0; i < s.length(); i++) {
      res.add(getKeyStroke(s.charAt(i)));
    }
    return res;
  }

  private enum KeyParserState {
    INIT,
    ESCAPE,
    SPECIAL,
  }

  /**
   * Parses Vim key notation strings.
   *
   * @throws java.lang.IllegalArgumentException if the mapping doesn't make sense for Vim emulation
   * @see :help <>
   */
  @NotNull
  public static List<KeyStroke> parseKeys(@NotNull String... strings) {
    final List<KeyStroke> result = new ArrayList<>();
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
            else if (c == '<' || c == '«') {
              state = KeyParserState.SPECIAL;
              specialKeyBuilder = new StringBuilder();
            }
            else {
              final KeyStroke stroke;
              if (c == '\t' || c == '\n') {
                stroke = getKeyStroke(c, 0);
              }
              else if (isControlCharacter(c)) {
                stroke = getKeyStroke(c + 'A' - 1, CTRL_MASK);
              }
              else {
                stroke = getKeyStroke(c);
              }
              result.add(stroke);
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
            if (c == '>' || c == '»') {
              state = KeyParserState.INIT;
              final String specialKeyName = specialKeyBuilder.toString();
              final String lower = specialKeyName.toLowerCase();
              if ("sid".equals(lower)) {
                throw new IllegalArgumentException("<" + specialKeyName + "> is not supported");
              }
              if ("comma".equals(lower)) {
                result.add(KeyStroke.getKeyStroke(','));
              }
              else if (!"nop".equals(lower)) {
                final List<KeyStroke> leader = parseMapLeader(specialKeyName);
                final KeyStroke specialKey = parseSpecialKey(specialKeyName, 0);
                if (leader != null) {
                  result.addAll(leader);
                }
                else if (specialKey != null) {
                  result.add(specialKey);
                }
                else {
                  result.add(getKeyStroke('<'));
                  result.addAll(stringToKeys(specialKeyName));
                  result.add(getKeyStroke('>'));
                }
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

  @Nullable
  private static List<KeyStroke> parseMapLeader(@NotNull String s) {
    if ("leader".equals(s.toLowerCase())) {
      final Object mapLeader = VimScriptGlobalEnvironment.getInstance().getVariables().get("mapleader");
      if (mapLeader instanceof String) {
        return stringToKeys((String)mapLeader);
      }
      else {
        return stringToKeys("\\");
      }
    }
    return null;
  }

  private static boolean isControlCharacter(char c) {
    return c < '\u0020';
  }

  private static boolean isControlKeyCode(@NotNull KeyStroke key) {
    return key.getKeyChar() == CHAR_UNDEFINED && key.getKeyCode() < 0x20 && key.getModifiers() == 0;
  }

  @NotNull
  public static String toKeyNotation(@NotNull List<KeyStroke> keys) {
    if (keys.isEmpty()) {
      return "<Nop>";
    }
    final StringBuilder builder = new StringBuilder();
    for (KeyStroke key : keys) {
      builder.append(toKeyNotation(key));
    }
    return builder.toString();
  }

  @NotNull
  public static String toKeyNotation(@NotNull KeyStroke key) {
    final char c = key.getKeyChar();
    final int keyCode = key.getKeyCode();
    final int modifiers = key.getModifiers();

    if (c != CHAR_UNDEFINED && !isControlCharacter(c)) {
      return String.valueOf(c);
    }

    String prefix = "";
    if ((modifiers & META_MASK) != 0) {
      prefix += "M-";
    }
    if ((modifiers & ALT_MASK) != 0) {
      prefix += "A-";
    }
    if ((modifiers & CTRL_MASK) != 0) {
      prefix += "C-";
    }
    if ((modifiers & SHIFT_MASK) != 0) {
      prefix += "S-";
    }

    String name = getVimKeyValue(keyCode);
    if (name != null) {
      if (containsDisplayUppercaseKeyNames(name)) {
        name = name.toUpperCase();
      }
      else {
        name = StringUtil.capitalize(name);
      }
    }
    if (name == null) {
      final String escape = toEscapeNotation(key);
      if (escape != null) {
        return escape;
      }

      try {
        name = String.valueOf(Character.toChars(keyCode));
      }
      catch (IllegalArgumentException ignored) {
      }
    }

    return name != null ? "<" + prefix + name + ">" : "<<" + key.toString() + ">>";
  }

  public static boolean containsUpperCase(@NotNull String text) {
    for (int i = 0; i < text.length(); i++) {
      if (Character.isUpperCase(text.charAt(i)) && (i == 0 || text.charAt(i - 1) != '\\')) {
        return true;
      }
    }

    return false;
  }

  public static boolean isCloseKeyStroke(@NotNull KeyStroke key) {
    return key.getKeyCode() == VK_ESCAPE ||
           key.getKeyChar() == VK_ESCAPE ||
           key.getKeyCode() == VK_C && (key.getModifiers() & CTRL_MASK) != 0 ||
           key.getKeyCode() == '[' && (key.getModifiers() & CTRL_MASK) != 0;
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

  @Nullable
  private static KeyStroke parseSpecialKey(@NotNull String s, int modifiers) {
    final String lower = s.toLowerCase();
    final Integer keyCode = getVimKeyName(lower);
    final Character typedChar = getVimTypedKeyName(lower);
    if (keyCode != null) {
      return getKeyStroke(keyCode, modifiers);
    }
    else if (typedChar != null) {
      return getTypedOrPressedKeyStroke(typedChar, modifiers);
    }
    else if (lower.startsWith(META_PREFIX)) {
      return parseSpecialKey(s.substring(META_PREFIX.length()), modifiers | META_MASK);
    }
    else if (lower.startsWith(ALT_PREFIX)) {
      return parseSpecialKey(s.substring(ALT_PREFIX.length()), modifiers | ALT_MASK);
    }
    else if (lower.startsWith(CTRL_PREFIX)) {
      return parseSpecialKey(s.substring(CTRL_PREFIX.length()), modifiers | CTRL_MASK);
    }
    else if (lower.startsWith(SHIFT_PREFIX)) {
      return parseSpecialKey(s.substring(SHIFT_PREFIX.length()), modifiers | SHIFT_MASK);
    }
    else if (s.length() == 1) {
      return getTypedOrPressedKeyStroke(s.charAt(0), modifiers);
    }
    return null;
  }

  private static boolean containsDisplayUppercaseKeyNames(String lower) {
    return "cr".equals(lower) || "bs".equals(lower);
  }

  private static Character getVimTypedKeyName(String lower) {
    switch (lower) {
      case "space":
        return ' ';
      case "bar":
        return '|';
      case "bslash":
        return '\\';
      case "lt":
        return '<';
      default:
        return null;
    }
  }

  private static Integer getVimKeyName(String lower) {
    switch (lower) {
      case "cr":
      case "enter":
      case "return":
        return VK_ENTER;
      case "ins":
      case "insert":
        return VK_INSERT;
      case "home":
        return VK_HOME;
      case "end":
        return VK_END;
      case "pageup":
        return VK_PAGE_UP;
      case "pagedown":
        return VK_PAGE_DOWN;
      case "del":
      case "delete":
        return VK_DELETE;
      case "esc":
        return VK_ESCAPE;
      case "bs":
        return VK_BACK_SPACE;
      case "backspace":
        return VK_BACK_SPACE;
      case "tab":
        return VK_TAB;
      case "up":
        return VK_UP;
      case "down":
        return VK_DOWN;
      case "left":
        return VK_LEFT;
      case "right":
        return VK_RIGHT;
      case "f1":
        return VK_F1;
      case "f2":
        return VK_F2;
      case "f3":
        return VK_F3;
      case "f4":
        return VK_F4;
      case "f5":
        return VK_F5;
      case "f6":
        return VK_F6;
      case "f7":
        return VK_F7;
      case "f8":
        return VK_F8;
      case "f9":
        return VK_F9;
      case "f10":
        return VK_F10;
      case "f11":
        return VK_F11;
      case "f12":
        return VK_F12;
      case "plug":
        return VK_PLUG;
      default:
        return null;
    }
  }

  private static String getVimKeyValue(int c) {
    switch (c) {
      case VK_ENTER:
        return "cr";
      case VK_INSERT:
        return "ins";
      case VK_HOME:
        return "home";
      case VK_END:
        return "end";
      case VK_PAGE_UP:
        return "pageup";
      case VK_PAGE_DOWN:
        return "pagedown";
      case VK_DELETE:
        return "del";
      case VK_ESCAPE:
        return "esc";
      case VK_BACK_SPACE:
        return "bs";
      case VK_TAB:
        return "tab";
      case VK_UP:
        return "up";
      case VK_DOWN:
        return "down";
      case VK_LEFT:
        return "left";
      case VK_RIGHT:
        return "right";
      case VK_F1:
        return "f1";
      case VK_F2:
        return "f2";
      case VK_F3:
        return "f3";
      case VK_F4:
        return "f4";
      case VK_F5:
        return "f5";
      case VK_F6:
        return "f6";
      case VK_F7:
        return "f7";
      case VK_F8:
        return "f8";
      case VK_F9:
        return "f9";
      case VK_F10:
        return "f10";
      case VK_F11:
        return "f11";
      case VK_F12:
        return "f12";
      case VK_PLUG:
        return "plug";
      default:
        return null;
    }
  }

  @NotNull
  private static KeyStroke getTypedOrPressedKeyStroke(char c, int modifiers) {
    if (modifiers == 0) {
      return getKeyStroke(c);
    }
    else if (modifiers == SHIFT_MASK) {
      return getKeyStroke(Character.toUpperCase(c));
    }
    else {
      return getKeyStroke(Character.toUpperCase(c), modifiers);
    }
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
   * Check if a char matches the Char production from the XML grammar.
   *
   * Characters beyond the Basic Multilingual Plane are not supported.
   */
  private static boolean isXmlChar(char c) {
    return '\u0001' <= c && c <= '\uD7FF' || '\uE000' <= c && c <= '\uFFFD';
  }
}
