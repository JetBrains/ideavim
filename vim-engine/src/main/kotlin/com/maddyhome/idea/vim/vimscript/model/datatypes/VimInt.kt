/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import java.util.*

data class VimInt(val value: Int) : VimDataType() {

  constructor(binaryOctalDecimalOrHexNumber: String) : this(parseNumber(binaryOctalDecimalOrHexNumber) ?: 0)

  override fun asDouble() = value.toDouble()
  override fun asString() = value.toString()

  override fun toVimNumber() = this
  override fun toVimString() = VimString(value.toString())

  override fun toOutputString() = value.toString()

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
    val MINUS_ONE: VimInt = VimInt(-1)
    val ZERO: VimInt = VimInt(0)
    val ONE: VimInt = VimInt(1)
  }
}

fun parseNumber(binaryOctalDecimalOrHexNumber: String): Int? {
  val n = binaryOctalDecimalOrHexNumber.lowercase(Locale.getDefault())
  return when {
    n.matches(Regex("-?0x[0-9a-f]+")) -> n.replaceFirst("0x", "").toInt(16)
    n.matches(Regex("-?0o?[0-7]+")) -> n.replaceFirst("0o", "").toInt(8)
    n.matches(Regex("-?0b[0-1]+")) -> n.replaceFirst("0b", "").toInt(2)
    n.matches(Regex("-?[0-9]+")) -> n.toInt()
    else -> null
  }
}

fun Boolean.asVimInt(): VimInt = if (this) VimInt.ONE else VimInt.ZERO

fun Int.asVimInt(): VimInt = when (this) {
  0 -> VimInt.ZERO
  1 -> VimInt.ONE
  else -> VimInt(this)
}
