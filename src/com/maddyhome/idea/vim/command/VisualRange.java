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

package com.maddyhome.idea.vim.command;

import org.jetbrains.annotations.NotNull;

public class VisualRange {
  final int myStart;
  final int myEnd;
  final int myOffset;
  @NotNull final SelectionType myType;

  public VisualRange(int start, int end, int offset, @NotNull SelectionType type) {
    myStart = start;
    myEnd = end;
    myType = type;
    myOffset = offset;
  }

  public int getStart() {
    return myStart;
  }

  public int getEnd() {
    return myEnd;
  }

  @NotNull
  public SelectionType getType() {
    return myType;
  }

  public int getOffset() {
    return myOffset;
  }

  @NotNull
  public String toString() {
    return "VisualRange[" + "start=" + myStart + ", end=" + myEnd + ", type=" + myType + ", offset=" + myOffset + "]";
  }
}
