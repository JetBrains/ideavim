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

  override fun toString(): String {
    val bigDecimal = BigDecimal(value).setScale(6, RoundingMode.HALF_UP)
    return bigDecimal.toDouble().toString()
  }
}
