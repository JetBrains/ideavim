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
 * Represents a Vim String
 *
 * This type has value semantics, i.e., two instances of [VimString] are considered equal if they have the same
 * underlying string value, based on Kotlin structural equality.
 *
 * Because I can never remember Kotlin's equality rules, especially with strings: this is a data class, so gets
 * generated `equals` and `hashCode` methods based on the string [value] property. The `equals` method uses Kotlin's
 * `Intrinsics.areEqual` method which will call `String.equals` if the objects are of the same type. This returns true
 * if the two strings are of the same length and have the same characters at the same positions. It is therefore
 * case-sensitive.
 *
 * A String can be automatically converted to a Number, but not a Float. The string value is parsed as a decimal, hex,
 * octal, or binary integer, and trailing characters are ignored. If the value can't be parsed, it is treated as zero.
 */
data class VimString(val value: String) : VimDataType("string") {
  override fun toVimFloat(): VimFloat {
    throw exExceptionMessage("E892")
  }

  override fun toVimNumber(): VimInt {
    // Vim will automatically convert a String to a Number when required during evaluation. The value is not parsed as
    // strictly as the text used to create a Number expression - Vim allows trailing characters for Number, so something
    // like `6bar` is parsed as `6`.
    return VimInt.parseNumber(value, allowTrailingCharacters = true) ?: VimInt.ZERO
  }

  override fun toVimString() = this
  override fun toOutputString() = value

  override fun valueEquals(other: VimDataType, ignoreCase: Boolean, depth: Int): Boolean {
    if (other !is VimString) return false
    return this.value.equals(other.value, ignoreCase = ignoreCase)
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
