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

package com.maddyhome.idea.vim.command;

import org.jetbrains.annotations.NotNull;

public class VisualRange {
  public VisualRange(int start, int end, @NotNull CommandState.SubMode type, int offset) {
    this.start = start;
    this.end = end;
    this.type = type;
    this.offset = offset;
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }

  @NotNull
  public CommandState.SubMode getType() {
    return type;
  }

  public int getOffset() {
    return offset;
  }

  @NotNull
  public String toString() {
    StringBuffer res = new StringBuffer();
    res.append("VisualRange[");
    res.append("start=").append(start);
    res.append(", end=").append(end);
    res.append(", type=").append(type);
    res.append(", offset=").append(offset);
    res.append("]");

    return res.toString();
  }

  int start;
  int end;
  @NotNull CommandState.SubMode type;
  int offset;
}
