/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.exExceptionMessage
import kotlin.text.appendLine

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

  val size: Int
    get() = values.size
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
    buildOutputString(this, mutableSetOf())
  }

  override fun buildOutputString(builder: StringBuilder, visited: MutableSet<VimDataType>) {
    if (visited.contains(this)) {
      builder.append("[...]")
      return
    }
    visited.add(this)
    builder.run {
      append("[")
      values.forEachIndexed { index, value ->
        if (index > 0) append(", ")
        if (value is VimString) {
          append("'").append(value.value).append("'")
        }
        else {
          value.buildOutputString(builder, visited)
        }
      }
      append("]")
    }
  }

  override fun toInsertableString() = buildString {
    values.forEach {
      if (it is VimString) {
        append("'").append(it.value).append("'")
      }
      else {
        it.buildInsertableString(this, 1)
      }
      appendLine()
    }
  }

  override fun buildInsertableString(builder: StringBuilder, depth: Int): Boolean {
    builder.run {
      return when (depth) {
        100 -> {
          append("[{E724}]")  // E724: Variable nested too deep for displaying
          false
        }

        else -> {
          append("[")
          var result = true
          var count = 0
          while (count < values.size) {
            val value = values[count]
            if (count > 0) append(", ")
            if (value is VimString) {
              append("'").append(value.value).append("'")
            } else {
              if (!value.buildInsertableString(builder, depth + 1)) {
                result = false
                break
              }
            }
            count++
          }
          append("]")
          result
        }
      }
    }
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
    if (other !is VimList) return false
    if (values.size != other.values.size) return false
    for (i in values.indices) {
      if (!values[i].valueEquals(other.values[i], ignoreCase, depth + 1)) return false
    }
    return true
  }

  override fun copy() = VimList(values.toMutableList())

  override fun deepCopy(useReferences: Boolean): VimList {
    val depth = 0
    val copiedReferences = if (useReferences) mutableMapOf<VimDataType, VimDataType>() else null
    return this.deepCopy(depth, copiedReferences)
  }

  override fun deepCopy(depth: Int, copiedReferences: MutableMap<VimDataType, VimDataType>?): VimList {
    // TODO: In Vim, I only see the check for nesting, not that referencing a higher level makes it fail
    // Nesting is possible up to 100 levels.  When there is an item
    // that refers back to a higher level making a deep copy with
    // {noref} set to 1 will fail.
    if (depth >= 100) {
      throw exExceptionMessage("E698")
    }
    copiedReferences?.get(this)?.let { return it as VimList }
    val newList = VimList(ArrayList(this.values.size))
    copiedReferences?.put(this, newList)
    values.forEach {
      newList.values.add(it.deepCopy(depth + 1, copiedReferences))
    }
    return newList
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
