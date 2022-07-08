package com.maddyhome.idea.vim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.parseNumber

/**
 * COMPATIBILITY-LAYER: Moved out of class and to a different package
 * Please see: https://jb.gg/zo8n0r
 */
open class NumberOption(name: String, abbrev: String, defaultValue: VimInt) :
  Option<VimInt>(name, abbrev, defaultValue) {
  constructor(name: String, abbrev: String, defaultValue: Int) : this(name, abbrev, VimInt(defaultValue))

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimInt) {
      throw ExException("E521: Number required after =: $token")
    }
  }

  override fun getValueIfAppend(currentValue: VimDataType, value: String, token: String): VimInt {
    val valueToAdd = parseNumber(token) ?: throw ExException("E521: Number required after =: $token")
    return VimInt((currentValue as VimInt).value + valueToAdd)
  }

  override fun getValueIfPrepend(currentValue: VimDataType, value: String, token: String): VimInt {
    val valueToAdd = parseNumber(token) ?: throw ExException("E521: Number required after =: $token")
    return VimInt((currentValue as VimInt).value * valueToAdd)
  }

  override fun getValueIfRemove(currentValue: VimDataType, value: String, token: String): VimInt {
    val valueToAdd = parseNumber(token) ?: throw ExException("E521: Number required after =: $token")
    return VimInt((currentValue as VimInt).value - valueToAdd)
  }

  fun value(): Int {
    return injector.optionService.getOptionValue(OptionScope.GLOBAL, name).asDouble().toInt()
  }
}
