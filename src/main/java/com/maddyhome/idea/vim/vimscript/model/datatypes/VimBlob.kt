/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.vimscript.model.datatypes

class VimBlob : VimDataType() {

  override fun asDouble(): Double {
    TODO("Not yet implemented")
  }

  override fun asString(): String {
    TODO("Not yet implemented")
  }

  override fun toVimNumber(): VimInt {
    TODO("Not yet implemented")
  }

  override fun asBoolean(): Boolean {
    TODO("empty must be falsy (0z), otherwise - truthy (like 0z00 0z01 etc)")
  }

  override fun deepCopy(level: Int): VimDataType {
    TODO("Not yet implemented")
  }

  override fun lockVar(depth: Int) {
    TODO("Not yet implemented")
  }

  override fun unlockVar(depth: Int) {
    TODO("Not yet implemented")
  }

  override fun toString(): String {
    TODO("Not yet implemented")
  }
}
