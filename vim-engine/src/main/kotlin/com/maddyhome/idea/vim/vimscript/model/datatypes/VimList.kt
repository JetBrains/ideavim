/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.exExceptionMessage

data class VimList(val values: MutableList<VimDataType>) : VimDataType() {

  operator fun get(index: Int): VimDataType = this.values[index]

  override fun asDouble(): Double {
    throw exExceptionMessage("E745")  // E745: Using a List as a Number
  }

  override fun asString(): String {
    throw exExceptionMessage("E730")  // E730: Using a List as a String
  }

  override fun toVimNumber(): VimInt {
    throw exExceptionMessage("E745")  // E745: Using a List as a Number
  }

  override fun toVimString(): VimString {
    throw exExceptionMessage("E730")  // E730: Using a List as a String
  }

  override fun toOutputString() = buildString {
    append("[")
    // TODO: Handle recursive references
    values.joinTo(this, separator = ", ") { if (it is VimString) "'${it.value}'" else it.toOutputString() }
    append("]")
  }

  override fun toInsertableString() = values.joinToString(separator = "") { it.toOutputString() + "\n" }

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
