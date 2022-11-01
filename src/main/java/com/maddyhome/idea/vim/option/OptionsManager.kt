/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionConstants.Companion.ignorecaseName
import com.maddyhome.idea.vim.options.OptionConstants.Companion.smartcaseName
import com.maddyhome.idea.vim.options.OptionConstants.Companion.timeoutName
import com.maddyhome.idea.vim.options.OptionConstants.Companion.timeoutlenName
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper
import com.maddyhome.idea.vim.vimscript.services.IjVimOptionService

/**
 * COMPATIBILITY-LAYER: Added a class and package
 * Please see: https://jb.gg/zo8n0r
 */
object OptionsManager {
  val ignorecase: ToggleOption
    get() = (injector.optionService as IjVimOptionService).getOptionByNameOrAbbr(ignorecaseName) as ToggleOption
  val smartcase: ToggleOption
    get() = (injector.optionService as IjVimOptionService).getOptionByNameOrAbbr(smartcaseName) as ToggleOption
  val timeout: ToggleOption
    get() = (injector.optionService as IjVimOptionService).getOptionByNameOrAbbr(timeoutName) as ToggleOption
  val timeoutlen: NumberOption
    get() = (injector.optionService as IjVimOptionService).getOptionByNameOrAbbr(timeoutlenName) as NumberOption
  val iskeyword: KeywordOption
    get() = KeywordOption(KeywordOptionHelper)
}

class KeywordOption(val helper: KeywordOptionHelper) {
  fun toRegex(): List<String> {
    return helper.toRegex()
  }
}

object StrictMode {
  fun fail(message: String) {
    if (injector.optionService.isSet(OptionScope.GLOBAL, OptionConstants.ideastrictmodeName)) {
      error(message)
    }
  }
}
