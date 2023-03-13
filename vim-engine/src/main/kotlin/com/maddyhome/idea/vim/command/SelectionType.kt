/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.command.VimStateMachine.SubMode

/**
 * @author vlan
 */
public enum class SelectionType(public val value: Int) {
  // Integer values for registers serialization in RegisterGroup.readData()
  LINE_WISE(1 shl 1),
  CHARACTER_WISE(1 shl 2),
  BLOCK_WISE(1 shl 3);

  public fun toSubMode(): SubMode = when (this) {
    LINE_WISE -> SubMode.VISUAL_LINE
    CHARACTER_WISE -> SubMode.VISUAL_CHARACTER
    BLOCK_WISE -> SubMode.VISUAL_BLOCK
  }

  public companion object {
    @JvmStatic
    public fun fromValue(value: Int): SelectionType {
      for (type in values()) {
        if (type.value == value) {
          return type
        }
      }
      return CHARACTER_WISE
    }

    @JvmStatic
    public fun fromSubMode(subMode: SubMode): SelectionType = when (subMode) {
      SubMode.VISUAL_LINE -> LINE_WISE
      SubMode.VISUAL_BLOCK -> BLOCK_WISE
      else -> CHARACTER_WISE
    }
  }
}

public val SelectionType.isLine: Boolean get() = this == SelectionType.LINE_WISE
public val SelectionType.isChar: Boolean get() = this == SelectionType.CHARACTER_WISE
public val SelectionType.isBlock: Boolean get() = this == SelectionType.BLOCK_WISE
