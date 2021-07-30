package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.ExException

data class VimList(val values: MutableList<VimDataType>) : VimDataType() {

  override fun asDouble(): Double {
    throw ExException("E745: Using a List as a Number")
  }

  override fun asString(): String {
    throw ExException("E730: Using a List as a String")
  }

  override fun toString(): String {
    val result = StringBuffer("[")
    result.append(values.joinToString(separator = ", ") { if (it is VimString) "'$it'" else it.toString() })
    result.append("]")
    return result.toString()
  }

  override fun asBoolean(): Boolean {
    throw ExException("E745: Using a List as a Number")
  }
}
