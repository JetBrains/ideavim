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

data class VimString(val value: String) : VimDataType() {

  // todo refactoring
  override fun asDouble(): Double {
    val text: String = value.toLowerCase()
    val intString = StringBuilder()

    if (text.startsWith("0x")) {
      var i = 2
      while (i < text.length && (text[i].isDigit() || text[i] in 'a'..'f')) {
        intString.append(text[i])
        ++i
      }
      return if (intString.isEmpty()) 0.0 else Integer.parseInt(intString.toString(), 16).toDouble()
    } else if (text.startsWith("-0x")) {
      var i = 3
      while (i < text.length && (text[i].isDigit() || text[i] in 'a'..'f')) {
        intString.append(text[i])
        ++i
      }
      return if (intString.isEmpty()) 0.0 else -Integer.parseInt(intString.toString(), 16).toDouble()
    } else if (text.startsWith("0")) {
      var i = 1
      while (i < text.length && text[i].isDigit()) {
        intString.append(text[i])
        ++i
      }
      return if (intString.isEmpty()) 0.0 else Integer.parseInt(intString.toString(), 8).toDouble()
    } else if (text.startsWith("-0")) {
      var i = 2
      while (i < text.length && text[i].isDigit()) {
        intString.append(text[i])
        ++i
      }
      return if (intString.isEmpty()) 0.0 else -Integer.parseInt(intString.toString(), 8).toDouble()
    } else if (text.startsWith("-")) {
      var i = 1
      while (i < text.length && text[i].isDigit()) {
        intString.append(text[i])
        ++i
      }
      return if (intString.isEmpty()) 0.0 else -Integer.parseInt(intString.toString()).toDouble()
    } else {
      var i = 0
      while (i < text.length && text[i].isDigit()) {
        intString.append(text[i])
        ++i
      }
      return if (intString.isEmpty()) 0.0 else Integer.parseInt(intString.toString()).toDouble()
    }
  }

  override fun asString(): String {
    return value
  }

  override fun toVimNumber(): VimInt = VimInt(this.value)

  override fun toString(): String {
    return value
  }

  override fun deepCopy(level: Int): VimString {
    return copy()
  }

  override fun lockVar(depth: Int) {
    this.isLocked = true
  }

  override fun unlockVar(depth: Int) {
    this.isLocked = false
  }
}
