package com.maddyhome.idea.vim.vimscript.model.datatypes

class VimBlob : VimDataType() {

  override fun asDouble(): Double {
    TODO("Not yet implemented")
  }

  override fun asString(): String {
    TODO("Not yet implemented")
  }

  override fun toVimNumber(): VimInt {
    TODO("Not yet implemented")
  }

  override fun asBoolean(): Boolean {
    TODO("empty must be falsy (0z), otherwise - truthy (like 0z00 0z01 etc)")
  }

  override fun toString(): String {
    TODO("Not yet implemented")
  }
}
