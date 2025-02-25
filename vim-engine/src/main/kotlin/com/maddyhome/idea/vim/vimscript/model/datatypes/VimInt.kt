/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

data class VimInt(val value: Int) : VimDataType() {

  constructor(binaryOctalDecimalOrHexNumber: String) : this(parseNumber(binaryOctalDecimalOrHexNumber) ?: 0)

  /**
   * Gets the value of the Number as a boolean, where zero is false and non-zero is true
   *
   * Vim represents boolean values as an instance of Number, where zero is false and non-zero is true. Only a Number can
   * be a boolean value, which means an expression must be a Number, or must be convertible to a Number (i.e., a String)
   * to be treated as a boolean.
   *
   * Use [toVimNumber] and [booleanValue] to get the evaluated result of an expression as a boolean value.
   */
  val booleanValue = value != 0

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

/**
 * Parses a string into a Vim Number, which is an integral value
 *
 * The string can be a binary (`0b`/`0B`), octal (`0o`/`0O`), decimal or hexadecimal (`0x`/`0X`) number. A string value
 * starting with `0` is an octal number, unless it contains the digits `8` or `9`, in which case, it's decimal. That is,
 * `015` is 13 decimal, but `019` is 19 decimal. The string value is case-insensitive.
 *
 * When parsing a string into a Number, e.g., while parsing the `80` from `:set textwidth=80`, then trailing characters
 * are not allowed, and will cause parsing to fail and return `null`.
 *
 * When converting a Vim String into a Number, trailing characters are allowed, e.g. `6bar` is parsed as `6`.
 *
 * @return The parsed integer value of the string, or `null` if trailing characters are present but not allowed. If
 *         trailing characters are allowed and there are no leading digits, then `0` is returned. E.g. `"foo"` => `0`.
 */
fun parseNumber(binaryOctalDecimalOrHexNumber: String, allowTrailingCharacters: Boolean = false): Int? {
  val n = binaryOctalDecimalOrHexNumber

  var index = 0
  val negative = if (n.startsWith("-")) { index++; true } else false
  val radix = when {
    n.startsWith("0x", index, true) -> { index += 2; 16 }
    n.startsWith("0o", index, true) -> { index += 2; 8 }
    n.startsWith("0b", index, true) -> { index += 2; 2 }
    n.matches(Regex("-?0[0-7]*[89]+.*")) -> { index += 1; 10 }  // E.g. 019 is decimal
    n.startsWith("0", index) -> { index += 1; 8 }
    else -> 10
  }

  var value = 0
  while (index < n.length) {
    val digit = n[index].digitToIntOrNull(radix)
    if (digit == null) {
      if (!allowTrailingCharacters) return null
      break
    }
    value = (value * radix) + digit
    index++
  }

  return if (negative) -value else value
}

fun Boolean.asVimInt(): VimInt = if (this) VimInt.ONE else VimInt.ZERO

fun Int.asVimInt(): VimInt = when (this) {
  0 -> VimInt.ZERO
  1 -> VimInt.ONE
  else -> VimInt(this)
}
