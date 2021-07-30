package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.ExException

data class VimDictionary(val dictionary: LinkedHashMap<VimString, VimDataType>) : VimDataType() {

  override fun asDouble(): Double {
    throw ExException("E728: Using a Dictionary as a Number")
  }

  override fun asString(): String {
    throw ExException("E731: Using a Dictionary as a String")
  }

  override fun toString(): String {
    val result = StringBuffer("{")
    result.append(dictionary.map { it.stringOfEntry() }.joinToString(separator = ", "))
    result.append("}")
    return result.toString()
  }

  private fun Map.Entry<VimString, VimDataType>.stringOfEntry(): String {
    val valueString = if (this.value is VimString) "'${this.value}'" else this.value.toString()
    return "'${this.key}': $valueString"
  }

  override fun asBoolean(): Boolean {
    throw ExException("E728: Using a Dictionary as a Number")
  }
}
