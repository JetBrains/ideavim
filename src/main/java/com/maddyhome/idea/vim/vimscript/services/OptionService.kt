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

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.options.Option
import com.maddyhome.idea.vim.vimscript.model.options.OptionChangeListener

interface OptionService {

  /**
   * todo doc for each method
   */
  fun getOptionValue(scope: Scope, optionName: String, token: String = optionName): VimDataType

  fun setOptionValue(scope: Scope, optionName: String, value: VimDataType, token: String = optionName)

  fun contains(scope: Scope, optionName: String, value: String): Boolean

  fun getValues(scope: Scope, optionName: String): List<String>?

  fun appendValue(scope: Scope, optionName: String, value: String, token: String = optionName)

  fun prependValue(scope: Scope, optionName: String, value: String, token: String = optionName)

  fun removeValue(scope: Scope, optionName: String, value: String, token: String = optionName)

  fun isSet(scope: Scope, optionName: String, token: String = optionName): Boolean

  fun isDefault(scope: Scope, optionName: String, token: String = optionName): Boolean

  fun resetDefault(scope: Scope, optionName: String, token: String = optionName)

  fun resetAllOptions()

  /**
   * Checks if the option with given optionName is a toggleOption
   */
  fun isToggleOption(optionName: String): Boolean

  /**
   * Sets the option on (true)
   */
  fun setOption(scope: Scope, optionName: String, token: String = optionName)

  /**
   * Unsets the option (false)
   */
  fun unsetOption(scope: Scope, optionName: String, token: String = optionName)

  fun toggleOption(scope: Scope, optionName: String, token: String = optionName)

  fun getOptions(): Set<String>

  fun getAbbrevs(): Set<String>

  fun addOption(option: Option<out VimDataType>)

  fun removeOption(optionName: String)

  // todo better generics
  fun addListener(optionName: String, listener: OptionChangeListener<VimDataType>, executeOnAdd: Boolean = false)

  fun removeListener(optionName: String, listener: OptionChangeListener<VimDataType>)

  sealed class Scope {
    object GLOBAL : Scope()
    class LOCAL(val editor: VimEditor) : Scope()
  }
}
