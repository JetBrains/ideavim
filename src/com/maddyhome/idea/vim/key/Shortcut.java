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

package com.maddyhome.idea.vim.key;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * This is a simple wrapper around a set of keystrokes. The various constructors make it easy to create the
 * keystroke set from simple characters and strings.
 */
public class Shortcut {
  /**
   * Create a shortcut containing one keystroke represented by the supplied character
   *
   * @param c The keystroke character
   */
  public Shortcut(char c) {
    keys = new KeyStroke[]{KeyStroke.getKeyStroke(c)};
  }

  /**
   * Creates a sequence of keystrokes represented by the characters of the supplied string
   *
   * @param keys The keystroke characters
   */
  public Shortcut(@NotNull String keys) {
    this.keys = new KeyStroke[keys.length()];
    for (int i = 0; i < keys.length(); i++) {
      this.keys[i] = KeyStroke.getKeyStroke(keys.charAt(i));
    }
  }

  /**
   * Creates a shortcut from the supplied keystroke
   *
   * @param key The keystroke
   */
  public Shortcut(KeyStroke key) {
    this.keys = new KeyStroke[]{key};
  }

  /**
   * Creates a shortcut based on the supplied list of keystrokes
   *
   * @param keys The keys
   */
  public Shortcut(KeyStroke[] keys) {
    this.keys = keys;
  }

  /**
   * Returns the list of keystrokes in this shortcut
   *
   * @return The keystroke list. The array should have at least one element
   */
  public KeyStroke[] getKeys() {
    return keys;
  }

  private KeyStroke[] keys;
}
