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

/**
 * Represents a Vim Float
 *
 * This type has value semantics. I.e., two instances of [VimFloat] are considered equal if they have the same
 * underlying value.
 *
 * A Vim Float cannot be converted to a Number or String.
 */
data class VimFloat(val value: Double) : VimDataType("float") {
  override fun toVimFloat() = this

  override fun toVimNumber(): VimInt {
    throw exExceptionMessage("E805")
  }

  override fun toVimString(): VimString {
    throw exExceptionMessage("E806")
  }

  override fun toOutputString(): String {
    if (value.isNaN()) return "nan"
    if (value.isInfinite()) return if (value > 0) "inf" else "-inf"
    val tooBigOrTooSmall = abs(value) >= 1e6 || (abs(value) < 1e-3 && value != 0.0)
    val pattern = if (tooBigOrTooSmall) "0.0#####E0" else "0.0#####"
    val symbols = DecimalFormatSymbols.getInstance(Locale.ROOT).apply {
      exponentSeparator = "e"
    }
    return DecimalFormat(pattern, symbols).format(value)
  }

  override fun copy() = VimFloat(value)

  override fun lockVar(depth: Int) {
    this.isLocked = true
  }

  override fun unlockVar(depth: Int) {
    this.isLocked = false
  }
}
