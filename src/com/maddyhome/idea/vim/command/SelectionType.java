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
