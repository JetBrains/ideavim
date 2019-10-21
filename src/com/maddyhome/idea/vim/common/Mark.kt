package com.maddyhome.idea.vim.common

data class Mark(val key: Char, var logicalLine: Int, val col: Int, val filename: String, val protocol: String) {

  private var cleared = false

  val isClear: Boolean
    get() = cleared

  fun clear() {
    cleared = true
  }
}

