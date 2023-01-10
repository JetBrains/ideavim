/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.ExException

data class VimList(val values: MutableList<VimDataType>) : VimDataType() {

  operator fun get(index: Int): VimDataType = this.values[index]

  override fun asDouble(): Double {
    throw ExException("E745: Using a List as a Number")
  }

  override fun asString(): String {
    throw ExException("E730: Using a List as a String")
  }

  override fun toVimNumber(): VimInt {
    throw ExException("E745: Using a List as a Number")
  }

  override fun toString(): String {
    val result = StringBuffer("[")
    result.append(values.joinToString(separator = ", ") { if (it is VimString) "'$it'" else it.toString() })
    result.append("]")
    return result.toString()
  }

  override fun asBoolean(): Boolean {
    throw ExException("E745: Using a List as a Number")
  }

  override fun deepCopy(level: Int): VimDataType {
    return if (level > 0) {
      VimList(values.map { it.deepCopy(level - 1) }.toMutableList())
    } else {
      this
    }
  }

  override fun lockVar(depth: Int) {
    this.isLocked = true
    if (depth > 1) {
      for (value in values) {
        value.lockVar(depth - 1)
      }
    }
  }

  override fun unlockVar(depth: Int) {
    this.isLocked = false
    if (depth > 1) {
      for (value in values) {
        value.unlockVar(depth - 1)
      }
    }
  }
}
