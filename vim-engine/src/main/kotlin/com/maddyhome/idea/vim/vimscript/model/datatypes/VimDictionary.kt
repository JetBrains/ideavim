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
 * Represents a Vim Dictionary
 *
 * This type does NOT have value semantics. It is not correct to compare two instances of this type for structural
 * equality. This is required so that recursive data structures don't cause problems with equality or hash codes.
 *
 * It cannot be converted to a Number, Float, or String. When output, any recursively used elements are replaced with a
 * placeholder. When inserted into a document as text, the value must be less than 100 levels deep, or an exception is
 * thrown.
 */
class VimDictionary(val dictionary: LinkedHashMap<VimString, VimDataType>) : VimDataType("dict") {
  override fun toVimFloat(): VimFloat {
    throw exExceptionMessage("E894")
  }

  override fun toVimNumber(): VimInt {
    throw exExceptionMessage("E728")
  }

  override fun toVimString(): VimString {
    throw exExceptionMessage("E731")
  }

  override fun toOutputString() = buildString {
    buildOutputString(this, mutableSetOf())
  }

  override fun buildOutputString(builder: StringBuilder, visited: MutableSet<VimDataType>) {
    if (visited.contains(this@VimDictionary)) {
      builder.append("{...}")
    }
    else {
      visited.add(this)
      builder.run {
        append("{")
        var count = 0
        dictionary.forEach { (key, value) ->
          if (count > 0) append(", ")
          if (value is VimString) {
            append("'${key.value}': '${value.value}'")
          } else {
            append("'${key.value}': ")
            value.buildOutputString(this, visited)
          }
          count++
        }
        append("}")
      }
    }
  }

  override fun toInsertableString() = buildString {
    buildInsertableString(this, 1)
  }

  override fun buildInsertableString(builder: StringBuilder, depth: Int): Boolean {
    if (depth == 100) {
      throw exExceptionMessage("E724")
    }
    builder.run {
      append("{")
      var count = 0
      dictionary.forEach { (key, value) ->
        if (count > 0) append(", ")
        append("'").append(key.value).append("'")
        if (value is VimString) {
          append("'").append(value.value).append("'")
        }
        else {
          value.buildInsertableString(this, depth + 1)
        }
        count++
      }
      append("}")
    }
    return true
  }

  override fun valueEquals(other: VimDataType, ignoreCase: Boolean, depth: Int): Boolean {
    // If the recursive structure is deep enough, treat it as equal.
    // The value is fairly arbitrary but based on Vim's behaviour. Vim will also reduce the limit for every comparison
    // once we're deep enough, so the tail of a list will both reduce the limit and short-circuit any further
    // comparisons, including potentially expensive nested comparisons.
    // So it should be possible to create a data structure that is 1001 levels deep (the first comparison is level 0)
    // but has different values in the tail of the list, and Vim would still treat it as equal.
    if (depth > 1000) return true

    if (this === other) return true
    if (other !is VimDictionary) return false
    if (dictionary.size != other.dictionary.size) return false
    return dictionary.all { (key, value) ->
      other.dictionary[key]?.valueEquals(value, ignoreCase, depth + 1) ?: false
    }
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
