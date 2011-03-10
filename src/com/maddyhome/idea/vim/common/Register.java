package com.maddyhome.idea.vim.common;

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

import com.maddyhome.idea.vim.helper.StringHelper;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a register.
 */
public class Register {
  /**
   * Create a register of the specified type for the given text
   *
   * @param name The character
   * @param type The register type (linewise or characterwise)
   * @param text The text to store
   */
  public Register(char name, int type, String text) {
    this.name = name;
    this.type = type;
    this.text = text;
    this.keys = null;
  }

  public Register(char name, int type, List<KeyStroke> keys) {
    this.name = name;
    this.type = type;
    this.text = null;
    this.keys = keys;
  }

  public void rename(char name) {
    this.name = name;
  }

  /**
   * Gets the name the register is assigned to
   *
   * @return The register name
   */
  public char getName() {
    return name;
  }

  /**
   * Get the register type
   *
   * @return The register type
   */
  public int getType() {
    return type;
  }

  /**
   * Get the text in the register
   *
   * @return The register text
   */
  public String getText() {
    if (text == null && keys != null) {
      return StringHelper.keysToString(keys);
    }
    return text;
  }

  /**
   * Get the sequence of keys in the register
   *
   * @return The register keys
   */
  public List<KeyStroke> getKeys() {
    if (keys == null && text != null) {
      return StringHelper.stringToKeys(text);
    }

    return keys;
  }

  /**
   * Appends the supplied text to any existing text
   *
   * @param text The text to add
   */
  public void addText(String text) {
    if (this.text != null) {
      this.text = this.text + text;
    }
    else if (this.keys != null) {
      addKeys(StringHelper.stringToKeys(text));
    }
    else {
      this.text = text;
    }
  }

  public void addKeys(List<KeyStroke> keys) {
    if (this.keys != null) {
      this.keys.addAll(keys);
    }
    else if (this.text != null) {
      this.text = this.text + StringHelper.keysToString(keys);
    }
    else {
      this.keys = keys;
    }
  }

  public boolean isText() {
    return text != null;
  }

  public boolean isKeys() {
    return keys != null;
  }

  public static class KeySorter<V> implements Comparator<V> {
    public int compare(V o1, V o2) {
      Register a = (Register)o1;
      Register b = (Register)o2;
      if (a.name < b.name) {
        return -1;
      }
      else if (a.name > b.name) {
        return 1;
      }
      else {
        return 0;
      }
    }
  }

  private char name;
  private int type;
  private String text;
  private List<KeyStroke> keys;
}
