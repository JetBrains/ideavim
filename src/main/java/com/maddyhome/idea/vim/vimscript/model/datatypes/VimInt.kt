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

data class VimInt(val value: Int) : VimDataType() {

  constructor(octalDecimalOrHexNumber: String) : this(parseNumber(octalDecimalOrHexNumber) ?: 0)

  override fun asDouble(): Double {
    return value.toDouble()
  }

  override fun asString(): String {
    return value.toString()
  }

  override fun toVimNumber(): VimInt = this

  override fun toString(): String {
    return value.toString()
  }

  override fun deepCopy(level: Int): VimInt {
    return copy()
  }

  override fun lockVar(depth: Int) {
    this.isLocked = true
  }

  override fun unlockVar(depth: Int) {
    this.isLocked = false
  }

  operator fun compareTo(b: Int): Int = this.value.compareTo(b)

  companion object {
    val ZERO = VimInt(0)
    val ONE = VimInt(1)
  }
}

fun parseNumber(octalDecimalOrHexNumber: String): Int? {
  val n = octalDecimalOrHexNumber.toLowerCase()
  return when {
    n.matches(Regex("[-]?0[x][0-9a-f]+")) -> n.replaceFirst("0x", "").toInt(16)
    n.matches(Regex("[-]?[0][0-7]+")) -> n.toInt(8)
    n.matches(Regex("[-]?[0-9]+")) -> n.toInt()
    else -> null
  }
}

fun Boolean.asVimInt(): VimInt = if (this) VimInt.ONE else VimInt.ZERO

fun Int.asVimInt(): VimInt = when (this) {
  0 -> VimInt.ZERO
  1 -> VimInt.ONE
  else -> VimInt(this)
}
