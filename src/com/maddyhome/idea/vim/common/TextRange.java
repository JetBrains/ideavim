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

package com.maddyhome.idea.vim.common;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class TextRange {
  public TextRange(int start, int end) {
    this(new int[]{start}, new int[]{end});
  }

  public TextRange(int[] starts, int[] ends) {
    this.starts = starts;
    this.ends = ends;
  }

  public boolean isMultiple() {
    return starts != null && starts.length > 1;
  }

  public int getMaxLength() {
    int max = 0;
    for (int i = 0; i < size(); i++) {
      max = Math.max(max, getEndOffsets()[i] - getStartOffsets()[i]);
    }

    return max;
  }

  public int getSelectionCount() {
    int res = 0;
    for (int i = 0; i < size(); i++) {
      res += getEndOffsets()[i] - getStartOffsets()[i];
    }

    return res;
  }

  public int size() {
    return starts.length;
  }

  public int getStartOffset() {
    return starts[0];
  }

  public int getEndOffset() {
    return ends[ends.length - 1];
  }

  public int[] getStartOffsets() {
    return starts;
  }

  public int[] getEndOffsets() {
    return ends;
  }

  @NotNull
  public TextRange normalize() {
    normalizeIndex(0);
    return this;
  }

  private void normalizeIndex(final int index) {
    if (index< size() && ends[index] < starts[index]) {
      int t = starts[index];
      starts[0] = ends[index];
      ends[index] = t;
    }
  }

  public boolean normalize(final int fileSize) {
    for (int i=0;i<size();i++) {
      normalizeIndex(i);
      starts[i] = Math.max(0, Math.min(starts[i], fileSize));
      if (starts[i] == fileSize) {
        return false;
      }
      ends[i] = Math.max(0, Math.min(ends[i], fileSize));
    }
    return true;
  }

  @NotNull
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("TextRange");
    sb.append("{starts=").append(starts == null ? "null" : "");
    for (int i = 0; starts != null && i < starts.length; ++i) {
      sb.append(i == 0 ? "" : ", ").append(starts[i]);
    }
    sb.append(", ends=").append(ends == null ? "null" : "");
    for (int i = 0; ends != null && i < ends.length; ++i) {
      sb.append(i == 0 ? "" : ", ").append(ends[i]);
    }
    sb.append('}');
    return sb.toString();
  }

  private final int[] starts;
  private final int[] ends;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TextRange textRange = (TextRange)o;

    if (!Arrays.equals(ends, textRange.ends)) return false;
    if (!Arrays.equals(starts, textRange.starts)) return false;

    return true;
  }
}