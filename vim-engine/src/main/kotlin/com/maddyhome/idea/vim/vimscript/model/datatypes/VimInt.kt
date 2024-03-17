/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import java.util.*

public data class VimInt(val value: Int) : VimDataType() {

  public constructor(octalDecimalOrHexNumber: String) : this(parseNumber(octalDecimalOrHexNumber) ?: 0)

  override fun asDouble(): Double {
    return value.toDouble()
  }

  override fun asString(): String {
    return value.toString()
  }

  override fun toVimNumber(): VimInt = this

  override fun toString(): String {
    return value.toString()
  }

  override fun deepCopy(level: Int): VimInt {
    return copy()
  }

  override fun lockVar(depth: Int) {
    this.isLocked = true
  }

  override fun unlockVar(depth: Int) {
    this.isLocked = false
  }

  public operator fun compareTo(b: Int): Int = this.value.compareTo(b)

  public companion object {
    public val MINUS_ONE: VimInt = VimInt(-1)
    public val ZERO: VimInt = VimInt(0)
    public val ONE: VimInt = VimInt(1)
  }
}

public fun parseNumber(octalDecimalOrHexNumber: String): Int? {
  val n = octalDecimalOrHexNumber.lowercase(Locale.getDefault())
  return when {
    n.matches(Regex("[-]?0[x][0-9a-f]+")) -> n.replaceFirst("0x", "").toInt(16)
    n.matches(Regex("[-]?[0][0-7]+")) -> n.toInt(8)
    n.matches(Regex("[-]?[0-9]+")) -> n.toInt()
    else -> null
  }
}

public fun Boolean.asVimInt(): VimInt = if (this) VimInt.ONE else VimInt.ZERO

public fun Int.asVimInt(): VimInt = when (this) {
  0 -> VimInt.ZERO
  1 -> VimInt.ONE
  else -> VimInt(this)
}
