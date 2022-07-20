/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
