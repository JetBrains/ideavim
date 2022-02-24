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

package com.maddyhome.idea.vim.vimscript.services

import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.options.Option
import com.maddyhome.idea.vim.options.OptionChangeListener

interface OptionService {

  /**
   * todo doc for each method
   */
  fun getOptionValue(scope: OptionScope, optionName: String, token: String = optionName): VimDataType

  fun setOptionValue(scope: OptionScope, optionName: String, value: VimDataType, token: String = optionName)

  fun contains(scope: OptionScope, optionName: String, value: String): Boolean

  fun getValues(scope: OptionScope, optionName: String): List<String>?

  fun appendValue(scope: OptionScope, optionName: String, value: String, token: String = optionName)

  fun prependValue(scope: OptionScope, optionName: String, value: String, token: String = optionName)

  fun removeValue(scope: OptionScope, optionName: String, value: String, token: String = optionName)

  fun isSet(scope: OptionScope, optionName: String, token: String = optionName): Boolean

  fun isDefault(scope: OptionScope, optionName: String, token: String = optionName): Boolean

  fun resetDefault(scope: OptionScope, optionName: String, token: String = optionName)

  fun resetAllOptions()

  /**
   * Checks if the option with given optionName is a toggleOption
   */
  fun isToggleOption(optionName: String): Boolean

  /**
   * Sets the option on (true)
   */
  fun setOption(scope: OptionScope, optionName: String, token: String = optionName)

  /**
   * Unsets the option (false)
   */
  fun unsetOption(scope: OptionScope, optionName: String, token: String = optionName)

  fun toggleOption(scope: OptionScope, optionName: String, token: String = optionName)

  fun getOptions(): Set<String>

  fun getAbbrevs(): Set<String>

  fun addOption(option: Option<out VimDataType>)

  fun removeOption(optionName: String)

  // todo better generics
  fun addListener(optionName: String, listener: OptionChangeListener<VimDataType>, executeOnAdd: Boolean = false)

  fun removeListener(optionName: String, listener: OptionChangeListener<VimDataType>)
}
