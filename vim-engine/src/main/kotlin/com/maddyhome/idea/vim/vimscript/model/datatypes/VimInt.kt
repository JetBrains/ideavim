/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

data class VimInt(val value: Int) : VimDataType() {
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

  /**
   * Returns the current object as a Vim Float, if possible (it's not)
   *
   * Vim will automatically convert between Number and String, but it does not convert from Number to Float (see
   * `:help Number`). However, there doesn't appear to be a distinct error for trying to use a Number as a Float, like
   * there is for other types (`E893: Using a List as a Float`). This seems to be because there is no way to get into
   * this situation.
   *
   * It is possible to call builtin functions with Number or Float, such as `pow()`, but the documentation states that
   * the expressions passed must evaluate to a Float or a Number, and will throw `E808: Number or Float required`, so
   * indicates that the function handler needs to check types rather than rely on something like `toVimFloat`.
   *
   * Note that when an expression mixes Float and Number (e.g., in an `if` condition), Vim will convert the Number to a
   * Float (see `:help E714` - "When mixing Number and Float the Number is converted to Float. Otherwise, there is no
   * automatic conversion of Float"). IdeaVim handles this conversion directly when evaluating expressions.
   */
  override fun toVimFloat(): VimFloat {
    error("Using a Number as a Float is not allowed")
  }

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
    // TODO: These constants are a ticking time bomb when combined with :lockvar
    // If we lock a List, and one of these constants is in there, it will be marked as locked for any other list that's
    // using the same content. We check the lock status of the item, not the parent container
    val MINUS_ONE: VimInt = VimInt(-1)
    val ZERO: VimInt = VimInt(0)
    val ONE: VimInt = VimInt(1)

    /**
     * Parses a string into a Vim Number, which is an integral value
     *
     * The string can be a binary (`0b`/`0B`), octal (`0o`/`0O`), decimal or hexadecimal (`0x`/`0X`) number. A string
     * value starting with `0` is an octal number, unless it contains the digits `8` or `9`, in which case, it's
     * decimal. That is, `015` is 13 decimal, but `019` is 19 decimal. The string value is case-insensitive.
     *
     * When parsing a string into a Number, e.g., while parsing the `80` from `:set textwidth=80`, then trailing
     * characters are not allowed, and will cause parsing to fail and return `null`.
     *
     * When converting a Vim String into a Number, trailing characters are allowed, e.g. `6bar` is parsed as `6`.
     *
     * @return The parsed integer value of the string, or `null` if trailing characters are present but not allowed. If
     *         trailing characters are allowed and there are no leading digits, then `0` is returned. E.g. `"foo"` => `0`.
     */
    fun parseNumber(binaryOctalDecimalOrHexNumber: String, allowTrailingCharacters: Boolean = false): VimInt? {
      var index = 0
      val negative = if (binaryOctalDecimalOrHexNumber.startsWith("-")) { index++; true } else false
      val radix = when {
        binaryOctalDecimalOrHexNumber.startsWith("0x", index, true) -> { index += 2; 16 }
        binaryOctalDecimalOrHexNumber.startsWith("0o", index, true) -> { index += 2; 8 }
        binaryOctalDecimalOrHexNumber.startsWith("0b", index, true) -> { index += 2; 2 }
        binaryOctalDecimalOrHexNumber.matches(Regex("-?0[0-7]*[89]+.*")) -> { index += 1; 10 }  // E.g. 019 is decimal
        binaryOctalDecimalOrHexNumber.startsWith("0", index) -> { index += 1; 8 }
        else -> 10
      }

      var value = 0
      while (index < binaryOctalDecimalOrHexNumber.length) {
        val digit = binaryOctalDecimalOrHexNumber[index].digitToIntOrNull(radix)
        if (digit == null) {
          if (!allowTrailingCharacters) return null
          break
        }
        value = (value * radix) + digit
        index++
      }

      // TODO: Using asVimInt will use a constant for
//      return (if (negative) -value else value).asVimInt()
      return if (negative) VimInt(-value) else VimInt(value)
    }
  }
}

fun Boolean.asVimInt(): VimInt = if (this) VimInt.ONE else VimInt.ZERO

fun Int.asVimInt(): VimInt = when (this) {
  0 -> VimInt.ZERO
  1 -> VimInt.ONE
  else -> VimInt(this)
}
