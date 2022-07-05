package com.maddyhome.idea.vim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

/**
 * COMPATIBILITY-LAYER: Moved out of class and to a different package
 */
class ToggleOption(name: String, abbrev: String, defaultValue: VimInt) : Option<VimInt>(name, abbrev, defaultValue) {
  constructor(name: String, abbrev: String, defaultValue: Boolean) : this(name, abbrev, if (defaultValue) VimInt.ONE else VimInt.ZERO)

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimInt) {
      throw ExException("E474: Invalid argument: $token")
    }
  }

  override fun getValueIfAppend(currentValue: VimDataType, value: String, token: String): VimInt {
    throw ExException("E474: Invalid argument: $token")
  }

  override fun getValueIfPrepend(currentValue: VimDataType, value: String, token: String): VimInt {
    throw ExException("E474: Invalid argument: $token")
  }

  override fun getValueIfRemove(currentValue: VimDataType, value: String, token: String): VimInt {
    throw ExException("E474: Invalid argument: $token")
  }

  /**
   * COMPATIBILITY-LAYER: Method added
   */
  fun isSet(): Boolean {
    return injector.optionService.getOptionValue(OptionScope.GLOBAL, name).asBoolean()
  }

  /**
   * COMPATIBILITY-LAYER: Method added
   */
  override val value: Boolean
    get() = injector.optionService.getOptionValue(OptionScope.GLOBAL, name).asBoolean()
}
