/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.command.CommandState.SubMode

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
