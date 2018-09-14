/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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
import com.intellij.openapi.util.text.StringUtil;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment;
import org.apache.commons.codec.binary.Base64;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.*;

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

  private static final Map<String, Integer> VIM_KEY_NAMES = ImmutableMap.<String, Integer>builder()
    .put("cr", VK_ENTER)
    .put("enter", VK_ENTER)
    .put("return", VK_ENTER)
    .put("ins", VK_INSERT)
    .put("insert", VK_INSERT)
    .put("home", VK_HOME)
    .put("end", VK_END)
    .put("pageup", VK_PAGE_UP)
    .put("pagedown", VK_PAGE_DOWN)
    .put("del", VK_DELETE)
    .put("delete", VK_DELETE)
    .put("esc", VK_ESCAPE)
    .put("bs", VK_BACK_SPACE)
    .put("backspace", VK_BACK_SPACE)
    .put("tab", VK_TAB)
    .put("up", VK_UP)
    .put("down", VK_DOWN)
    .put("left", VK_LEFT)
    .put("right", VK_RIGHT)
    .put("f1", VK_F1)
    .put("f2", VK_F2)
    .put("f3", VK_F3)
    .put("f4", VK_F4)
    .put("f5", VK_F5)
    .put("f6", VK_F6)
    .put("f7", VK_F7)
    .put("f8", VK_F8)
    .put("f9", VK_F9)
    .put("f10", VK_F10)
    .put("f11", VK_F11)
    .put("f12", VK_F12)
    .put("plug", VK_PLUG)
    .build();
  private static final Map<Integer, String> VIM_KEY_VALUES = invertMap(VIM_KEY_NAMES);

  private static final Map<String, Character> VIM_TYPED_KEY_NAMES = ImmutableMap.<String, Character>builder()
    .put("space", ' ')
    .put("bar", '|')
    .put("bslash", '\\')
    .put("lt", '<')
    .build();

  private static final Set<String> UPPERCASE_DISPLAY_KEY_NAMES = ImmutableSet.<String>builder()
    .add("cr")
    .add("bs")
    .build();

  private StringHelper() {}

  @NotNull
  public static String leftJustify(@NotNull String text, int width, char fillChar) {
    final StringBuilder builder = new StringBuilder(text);
    for (int i = text.length(); i < width; i++) {
      builder.append(fillChar);
    }
    return builder.toString();
  }

  @NotNull
  public static String rightJustify(@NotNull String text, int width, char fillChar) {
    final StringBuilder builder = new StringBuilder(text);
    for (int i = text.length(); i < width; i++) {
      builder.insert(0, fillChar);
    }
    return builder.toString();
  }

  @Nullable
  private static String toEscapeNotation(@NotNull KeyStroke key) {
    final char c = key.getKeyChar();
    if (isControlCharacter(c)) {
      return "^" + String.valueOf((char)(c + 'A' - 1));
    }
    else if (isControlKeyCode(key)) {
      return "^" + String.valueOf((char)(key.getKeyCode() + 'A' - 1));
    }
    return null;
  }

  @NotNull
  public static List<KeyStroke> stringToKeys(@NotNull String s) {
    final List<KeyStroke> res = new ArrayList<KeyStroke>();
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
            if (c == '>') {
              state = KeyParserState.INIT;
              final String specialKeyName = specialKeyBuilder.toString();
              final String lower = specialKeyName.toLowerCase();
              if ("sid".equals(lower)) {
                throw new IllegalArgumentException("<" + specialKeyName + "> is not supported");
              }
              if (!"nop".equals(lower)) {
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

    String name = VIM_KEY_VALUES.get(keyCode);
    if (name != null) {
      if (UPPERCASE_DISPLAY_KEY_NAMES.contains(name)) {
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

  @NotNull
  private static <K, V> Map<V, K> invertMap(@NotNull Map<K, V> map) {
    final Map<V, K> inverted = new HashMap<V, K>();
    for (Map.Entry<K, V> entry : map.entrySet()) {
      final V value = entry.getValue();
      if (!inverted.containsKey(value)) {
        inverted.put(value, entry.getKey());
      }
    }
    return inverted;
  }

  @Nullable
  private static KeyStroke parseSpecialKey(@NotNull String s, int modifiers) {
    final String lower = s.toLowerCase();
    final Integer keyCode = VIM_KEY_NAMES.get(lower);
    final Character typedChar = VIM_TYPED_KEY_NAMES.get(lower);
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
