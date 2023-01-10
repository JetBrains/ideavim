/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.ExException
import java.math.BigDecimal
import java.math.RoundingMode

data class VimFloat(val value: Double) : VimDataType() {

  override fun asDouble(): Double {
    return value
  }

  override fun asString(): String {
    throw ExException("E806: using Float as a String")
  }

  override fun toVimNumber(): VimInt {
    throw ExException("E805: Using a Float as a Number")
  }

  override fun toString(): String {
    val bigDecimal = BigDecimal(value).setScale(6, RoundingMode.HALF_UP)
    return bigDecimal.toDouble().toString()
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
