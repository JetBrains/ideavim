/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.exExceptionMessage

data class VimDictionary(val dictionary: LinkedHashMap<VimString, VimDataType>) : VimDataType() {

  override fun asDouble(): Double {
    throw exExceptionMessage("E728")  // E728: Using a Dictionary as a Number
  }

  override fun asString(): String {
    throw exExceptionMessage("E731")  // E731: Using a Dictionary as a String
  }

  override fun toVimNumber(): VimInt {
    throw exExceptionMessage("E728")  // E728: Using a Dictionary as a Number
  }

  override fun toVimString(): VimString {
    throw exExceptionMessage("E731")  // E731: Using a Dictionary as a String
  }

  override fun toString(): String {
    val result = StringBuffer("{")
    result.append(dictionary.map { stringOfEntry(it) }.joinToString(separator = ", "))
    result.append("}")
    return result.toString()
  }

  private fun stringOfEntry(entry: Map.Entry<VimString, VimDataType>): String {
    val valueString = when (entry.value) {
      is VimString -> "'${entry.value}'"
      else -> entry.value.toString()
    }
    return "'${entry.key}': $valueString"
  }

  override fun asBoolean(): Boolean {
    throw exExceptionMessage("E728")  // E728: Using a Dictionary as a Number
  }

  override fun deepCopy(level: Int): VimDictionary {
    return if (level > 0) {
      VimDictionary(linkedMapOf(*(dictionary.map { it.key.copy() to it.value.deepCopy(level - 1) }.toTypedArray())))
    } else {
      this
    }
  }

  override fun lockVar(depth: Int) {
    this.isLocked = true
    if (depth > 1) {
      for (value in dictionary.values) {
        value.lockVar(depth - 1)
      }
    }
  }

  override fun unlockVar(depth: Int) {
    this.isLocked = false
    if (depth > 1) {
      for (value in dictionary.values) {
        value.unlockVar(depth - 1)
      }
    }
  }
}
