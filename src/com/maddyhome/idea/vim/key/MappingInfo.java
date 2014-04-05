/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2014 The IdeaVim authors
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

import com.maddyhome.idea.vim.command.MappingMode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Set;

/**
 * @author vlan
 */
public class MappingInfo implements Comparable<MappingInfo> {
  @NotNull private final Set<MappingMode> myMappingModes;
  @NotNull private final List<KeyStroke> myFromKeys;
  @NotNull private final List<KeyStroke> myToKeys;
  private final boolean myRecursive;

  public MappingInfo(@NotNull Set<MappingMode> mappingModes, @NotNull List<KeyStroke> fromKeys,
                     @NotNull List<KeyStroke> toKeys, boolean recursive) {
    myMappingModes = mappingModes;
    myFromKeys = fromKeys;
    myToKeys = toKeys;
    myRecursive = recursive;
  }

  @Override
  public int compareTo(@NotNull MappingInfo other) {
    final int size = myFromKeys.size();
    final int otherSize = other.myFromKeys.size();
    final int n = Math.min(size, otherSize);
    for (int i = 0; i < n; i++) {
      final int diff = compareKeys(myFromKeys.get(i), other.myFromKeys.get(i));
      if (diff != 0) {
        return diff;
      }
    }
    return size - otherSize;
  }

  @NotNull
  public Set<MappingMode> getMappingModes() {
    return myMappingModes;
  }

  @NotNull
  public List<KeyStroke> getFromKeys() {
    return myFromKeys;
  }

  @NotNull
  public List<KeyStroke> getToKeys() {
    return myToKeys;
  }

  public boolean isRecursive() {
    return myRecursive;
  }

  private int compareKeys(@NotNull KeyStroke key1, @NotNull KeyStroke key2) {
    final char c1 = key1.getKeyChar();
    final char c2 = key2.getKeyChar();
    if (c1 == KeyEvent.CHAR_UNDEFINED && c2 == KeyEvent.CHAR_UNDEFINED) {
      final int keyCodeDiff = key1.getKeyCode() - key2.getKeyCode();
      if (keyCodeDiff != 0) {
        return keyCodeDiff;
      }
      return key1.getModifiers() - key2.getModifiers();
    }
    else if (c1 == KeyEvent.CHAR_UNDEFINED) {
      return -1;
    }
    else if (c2 == KeyEvent.CHAR_UNDEFINED) {
      return 1;
    }
    else {
      return c1 - c2;
    }
  }
}
