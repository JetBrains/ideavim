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

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.ExException
import java.math.BigDecimal
import java.math.RoundingMode

data class VimFloat(val value: Double) : VimDataType() {

  override fun asDouble(): Double {
    return value
  }

  override fun asString(): String {
    throw ExException("E806: using Float as a String")
  }

  override fun toVimNumber(): VimInt {
    throw ExException("E805: Using a Float as a Number")
  }

  override fun toString(): String {
    val bigDecimal = BigDecimal(value).setScale(6, RoundingMode.HALF_UP)
    return bigDecimal.toDouble().toString()
  }

  override fun deepCopy(level: Int): VimFloat {
    return copy()
  }

  override fun lockVar(depth: Int) {
    this.isLocked = true
  }

  override fun unlockVar(depth: Int) {
    this.isLocked = false
  }
}
