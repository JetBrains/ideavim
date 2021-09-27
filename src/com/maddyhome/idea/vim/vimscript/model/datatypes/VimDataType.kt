package com.maddyhome.idea.vim.vimscript.model.datatypes

sealed class VimDataType {

  abstract fun asDouble(): Double

  // string value that is used in arithmetic expressions (concatenation etc.)
  abstract fun asString(): String

  abstract fun toVimNumber(): VimInt

  // string value that is used in echo-like commands
  override fun toString(): String {
    throw NotImplementedError("implement me :(")
  }

  open fun asBoolean(): Boolean {
    return asDouble() != 0.0
  }
}
