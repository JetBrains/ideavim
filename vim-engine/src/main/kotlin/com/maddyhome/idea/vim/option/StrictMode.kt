package com.maddyhome.idea.vim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope


@Deprecated("Please use StrictMode from com.maddyhome.idea.vim.options.helpers", replaceWith = ReplaceWith("com.maddyhome.idea.vim.options.helpers.StrictMode"))
object StrictMode {
  @JvmName("assertTrue")
  fun assert(condition: Boolean, message: String) {
    if (!condition) {
      fail(message)
    }
  }

  fun fail(message: String) {
    if (injector.optionService.isSet(OptionScope.GLOBAL, OptionConstants.ideastrictmodeName)) {
      error(message)
    }
  }
}
