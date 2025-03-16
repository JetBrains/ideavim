/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import java.util.*

data class VimString(val value: String) : VimDataType() {

  // todo refactoring
  override fun asDouble(): Double {
    val text: String = value.lowercase(Locale.getDefault())
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
  override fun toVimString(): VimString = this

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

  companion object {
    val EMPTY: VimString = VimString("")
  }
}
