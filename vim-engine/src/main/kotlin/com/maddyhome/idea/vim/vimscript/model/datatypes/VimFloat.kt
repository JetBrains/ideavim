/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.exExceptionMessage
import java.text.DecimalFormat
import kotlin.math.abs

data class VimFloat(val value: Double) : VimDataType() {

  override fun asDouble(): Double {
    return value
  }

  override fun asString(): String {
    throw exExceptionMessage("E806") // E806: Using a Float as a String
  }

  override fun toVimNumber(): VimInt {
    throw exExceptionMessage("E805") // E805: Using a Float as a Number
  }

  override fun toVimString(): VimString {
    throw exExceptionMessage("E806") // E806: Using a Float as a String
  }

  override fun toString(): String {
    if (value.isNaN()) return "nan"
    return if (abs(value) >= 1e6 || (abs(value) < 1e-3 && value != 0.0)) {
      val formatter = DecimalFormat("0.0#####E0")
      formatter.decimalFormatSymbols = formatter.decimalFormatSymbols.apply { exponentSeparator = "e" }
      formatter.format(value)
    }
    else {
      DecimalFormat("0.0#####").format(value)
    }
  }

  override fun deepCopy(level: Int): VimFloat {
    return copy()
  }

  override fun lockVar(depth: Int) {
    this.isLocked = true
  }

  override fun unlockVar(depth: Int) {
    this.isLocked = false
  }
}
