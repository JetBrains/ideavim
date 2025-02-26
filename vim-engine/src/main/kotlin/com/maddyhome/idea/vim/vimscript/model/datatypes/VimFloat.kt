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
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

data class VimFloat(val value: Double) : VimDataType() {

  override fun asDouble(): Double {
    return value
  }

  override fun toVimNumber(): VimInt {
    throw exExceptionMessage("E805") // E805: Using a Float as a Number
  }

  override fun toVimString(): VimString {
    throw exExceptionMessage("E806") // E806: Using a Float as a String
  }

  override fun toOutputString(): String {
    if (value.isNaN()) return "nan"
    val tooBigOrTooSmall = abs(value) >= 1e6 || (abs(value) < 1e-3 && value != 0.0)
    val pattern = if (tooBigOrTooSmall) "0.0#####E0" else "0.0#####"
    val symbols = DecimalFormatSymbols.getInstance(Locale.ROOT).apply {
      exponentSeparator = "e"
    }
    return DecimalFormat(pattern, symbols).format(value)
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
