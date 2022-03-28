package com.maddyhome.idea.vim.common

enum class Direction(private val value: Int) {
  BACKWARDS(-1), FORWARDS(1);

  fun toInt(): Int = value
  fun reverse(): Direction = when (this) {
    BACKWARDS -> FORWARDS
    FORWARDS -> BACKWARDS
  }

  companion object {
    fun fromInt(value: Int) = when (value) {
      BACKWARDS.value -> BACKWARDS
      FORWARDS.value -> FORWARDS
      else -> FORWARDS
    }
  }
}
