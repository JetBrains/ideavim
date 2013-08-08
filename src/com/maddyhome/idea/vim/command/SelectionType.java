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

/**
 * @author vlan
 */
public enum SelectionType {
  // Integer values for registers serialization in RegisterGroup.readData()
  LINE_WISE(1 << 1),
  CHARACTER_WISE(1 << 2),
  BLOCK_WISE(1 << 3);

  SelectionType(int value) {
    this.value = value;
  }

  private final int value;

  public int getValue() {
    return value;
  }

  @NotNull
  public static SelectionType fromValue(int value) {
    for (SelectionType type : SelectionType.values()) {
      if (type.getValue() == value) {
        return type;
      }
    }
    return CHARACTER_WISE;
  }

  @NotNull
  public static SelectionType fromSubMode(@NotNull CommandState.SubMode subMode) {
    switch (subMode) {
      case VISUAL_LINE:
        return LINE_WISE;
      case VISUAL_BLOCK:
        return BLOCK_WISE;
      default:
        return CHARACTER_WISE;
    }
  }

  @NotNull
  public static SelectionType fromCommandFlags(int flags) {
    if ((flags & Command.FLAG_MOT_LINEWISE) != 0) {
      return SelectionType.LINE_WISE;
    }
    else if ((flags & Command.FLAG_MOT_BLOCKWISE) != 0) {
      return SelectionType.BLOCK_WISE;
    }
    else {
      return SelectionType.CHARACTER_WISE;
    }
  }
}
