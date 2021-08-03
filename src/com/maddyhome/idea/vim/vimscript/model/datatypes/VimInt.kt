package com.maddyhome.idea.vim.vimscript.model.datatypes

data class VimInt(val value: Int) : VimDataType() {

  constructor(octalDecimalOrHexNumber: String) : this(parseNumber(octalDecimalOrHexNumber))

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

  companion object {
    val ZERO = VimInt(0)
    val ONE = VimInt(1)
  }
}

private fun parseNumber(octalDecimalOrHexNumber: String): Int {
  val n = octalDecimalOrHexNumber.toLowerCase()
  return when {
    n.matches(Regex("[-]?0[x][0-9a-f]+")) -> n.replaceFirst("0x", "").toInt(16)
    n.matches(Regex("[-]?[0][0-7]+")) -> n.toInt(8)
    n.matches(Regex("[-]?[0-9]+")) -> n.toInt()
    else -> 0
  }
}

fun Boolean.asVimInt(): VimInt = if (this) VimInt.ONE else VimInt.ZERO

fun Int.asVimInt(): VimInt = when (this) {
  0 -> VimInt.ZERO
  1 -> VimInt.ONE
  else -> VimInt(this)
}
