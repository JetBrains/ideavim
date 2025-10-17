/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.exExceptionMessage

/**
 * Represents a Vim List
 *
 * This type does NOT have value semantics. It is not correct to compare two instances of this type for structural
 * equality. This is required so that recursive data structures don't cause problems with equality or hash codes.
 *
 * It cannot be converted to a Number, Float, or String. When output, any recursively used elements are replaced with a
 * placeholder. When inserted into a document as text, the value must be less than 100 levels deep, or an exception is
 * thrown.
 */
class VimList(val values: MutableList<VimDataType>) : VimDataType("list") {
  operator fun get(index: Int): VimDataType = this.values[index]

  override fun toVimFloat(): VimFloat {
    throw exExceptionMessage("E893")
  }

  override fun toVimNumber(): VimInt {
    throw exExceptionMessage("E745")
  }

  override fun toVimString(): VimString {
    throw exExceptionMessage("E730")
  }

  override fun toOutputString() = buildString {
    append("[")
    // TODO: Handle recursive references
    values.joinTo(this, separator = ", ") { if (it is VimString) "'${it.value}'" else it.toOutputString() }
    append("]")
  }

  override fun toInsertableString() = values.joinToString(separator = "") { it.toOutputString() + "\n" }

  override fun deepCopy(level: Int): VimList {
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
