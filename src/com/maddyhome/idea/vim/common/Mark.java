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

package com.maddyhome.idea.vim.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * This represents a file mark. Each mark has a line and a column, the file it applies to, and the mark key
 */
public class Mark extends FileLocation {
  /**
   * Creates a file mark
   *
   * @param key      The mark's key
   * @param lline    The logical line within the file
   * @param col      The column within the line
   * @param filename The file being marked
   */
  public Mark(char key, int lline, int col, String filename) {
    super(lline, col, filename);

    this.key = key;
  }

  /**
   * The mark's key
   *
   * @return The mark's key
   */
  public char getKey() {
    return key;
  }

  public boolean equals(@Nullable Object object) {
    if (object instanceof Mark) {
      if (((Mark)object).key == key) {
        return true;
      }
    }

    return false;
  }

  @NotNull
  public String toString() {
    final StringBuffer sb = new StringBuffer();
    sb.append("Mark{");
    sb.append(super.toString());
    sb.append(",key=").append(key);
    sb.append('}');
    return sb.toString();
  }

  public static class KeySorter<V> implements Comparator<V> {
    public int compare(V o1, V o2) {
      Mark a = (Mark)o1;
      Mark b = (Mark)o2;
      if (a.key < b.key) {
        return -1;
      }
      else if (a.key > b.key) {
        return 1;
      }
      else {
        return 0;
      }
    }
  }

  private char key;
}
