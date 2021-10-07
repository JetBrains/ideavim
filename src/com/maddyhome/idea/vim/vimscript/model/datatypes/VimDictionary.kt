package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.ExException

data class VimDictionary(val dictionary: LinkedHashMap<VimString, VimDataType>) : VimDataType() {

  override fun asDouble(): Double {
    throw ExException("E728: Using a Dictionary as a Number")
  }

  override fun asString(): String {
    throw ExException("E731: Using a Dictionary as a String")
  }

  override fun toVimNumber(): VimInt {
    throw ExException("E728: Using a Dictionary as a Number")
  }

  override fun toString(): String {
    val result = StringBuffer("{")
    result.append(dictionary.map { stringOfEntry(it) }.joinToString(separator = ", "))
    result.append("}")
    return result.toString()
  }

  private fun stringOfEntry(entry: Map.Entry<VimString, VimDataType>): String {
    val valueString = when (entry.value) {
      is VimString -> "'${entry.value}'"
      else -> entry.value.toString()
    }
    return "'${entry.key}': $valueString"
  }

  override fun asBoolean(): Boolean {
    throw ExException("E728: Using a Dictionary as a Number")
  }
}
