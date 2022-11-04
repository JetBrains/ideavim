/*
 * Copyright 2003-2022 The IdeaVim authors
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
enum class SelectionType(val value: Int) {
  // Integer values for registers serialization in RegisterGroup.readData()
  LINE_WISE(1 shl 1),
  CHARACTER_WISE(1 shl 2),
  BLOCK_WISE(1 shl 3);

  fun toSubMode() = when (this) {
    LINE_WISE -> SubMode.VISUAL_LINE
    CHARACTER_WISE -> SubMode.VISUAL_CHARACTER
    BLOCK_WISE -> SubMode.VISUAL_BLOCK
  }

  companion object {
    @JvmStatic
    fun fromValue(value: Int): SelectionType {
      for (type in values()) {
        if (type.value == value) {
          return type
        }
      }
      return CHARACTER_WISE
    }

    @JvmStatic
    fun fromSubMode(subMode: SubMode): SelectionType = when (subMode) {
      SubMode.VISUAL_LINE -> LINE_WISE
      SubMode.VISUAL_BLOCK -> BLOCK_WISE
      else -> CHARACTER_WISE
    }
  }
}

val SelectionType.isLine get() = this == SelectionType.LINE_WISE
val SelectionType.isChar get() = this == SelectionType.CHARACTER_WISE
val SelectionType.isBlock get() = this == SelectionType.BLOCK_WISE
