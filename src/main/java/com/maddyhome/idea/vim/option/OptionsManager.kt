/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.option

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.helpers.KeywordOptionHelper
import com.maddyhome.idea.vim.vimscript.services.IjVimOptionService

/**
 * COMPATIBILITY-LAYER: Added a class and package
 * Please see: https://jb.gg/zo8n0r
 */
object OptionsManager {
  val ignorecase: ToggleOption
    get() = injector.optionService.getOptionByNameOrAbbr(OptionConstants.ignorecase) as ToggleOption
  val smartcase: ToggleOption
    get() = injector.optionService.getOptionByNameOrAbbr(OptionConstants.smartcase) as ToggleOption
  val timeout: ToggleOption
    get() = injector.optionService.getOptionByNameOrAbbr(OptionConstants.timeout) as ToggleOption
  val timeoutlen: NumberOption
    get() = injector.optionService.getOptionByNameOrAbbr(OptionConstants.timeoutlen) as NumberOption
  val iskeyword: KeywordOption
    get() = KeywordOption(KeywordOptionHelper)
}

class KeywordOption(val helper: KeywordOptionHelper) {
  fun toRegex(): List<String> {
    return helper.toRegex()
  }
}
