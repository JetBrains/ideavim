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
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionChangeListener
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

/**
 * COMPATIBILITY-LAYER: Moved to a different package
 * Please see: https://jb.gg/zo8n0r
 */
interface OptionService {

  /**
   * Gets option value.
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param token used in exception messages
   * @throws ExException("E518: Unknown option: $token")
   */
  fun getOptionValue(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, token: String = optionName): VimDataType

  /**
   * Sets option value.
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param value option value
   * @param token used in exception messages
   * @throws ExException("E518: Unknown option: $token")
   */
  fun setOptionValue(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, value: VimDataType, token: String = optionName)

  /**
   * Checks if the [value] is contained in string option.
   *
   * Returns false if there is no option with the given optionName, or it's type is different from string.
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param value option value
   */
  fun contains(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, value: String): Boolean

  /**
   * Splits a string option into flags
   *
   * e.g. the `fileencodings` option with value "ucs-bom,utf-8,default,latin1" will result listOf("ucs-bom", "utf-8", "default", "latin1")
   *
   * returns null if there is no option with the given optionName, or its type is different from string.
   * @param scope global/local option scope
   * @param optionName option name or alias
   */
  fun getValues(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String): List<String>?

  /**
   * Same as [setOptionValue], but automatically casts [value] to the required [VimDataType]
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param value option value
   * @param token used in exception messages
   * @throws ExException("E518: Unknown option: $token") in case the option is not found
   * @throws ExException("E474: Invalid argument: $token") in case the cast to VimDataType is impossible
   */
  fun setOptionValue(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, value: String, token: String = optionName)

  /**
   * Same as `set {option}+={value}` in Vim documentation.
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param value option value
   * @param token used in exception messages
   * @throws ExException("E518: Unknown option: $token") in case the option is not found
   * @throws ExException("E474: Invalid argument: $token") in case the method was called for the [ToggleOption]
   * @throws ExException("E474: Invalid argument: $token") in case the method was called for the [StringOption] and the argument is invalid (does not satisfy the option bounded values)
   * @throws ExException("E521: Number required after =: $token") in case the cast to VimInt is impossible
   */
  fun appendValue(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, value: String, token: String = optionName)

  /**
   * Same as `set {option}^={value}` in Vim documentation.
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param value option value
   * @param token used in exception messages
   * @throws ExException("E518: Unknown option: $token") in case the option is not found
   * @throws ExException("E474: Invalid argument: $token") in case the method was called for the [ToggleOption]
   * @throws ExException("E474: Invalid argument: $token") in case the method was called for the [StringOption] and the argument is invalid (does not satisfy the option bounded values)
   * @throws ExException("E521: Number required after =: $token") in case the cast to VimInt is impossible
   */
  fun prependValue(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, value: String, token: String = optionName)

  /**
   * Same as `set {option}-={value}` in Vim documentation.
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param value option value
   * @param token used in exception messages
   * @throws ExException("E518: Unknown option: $token") in case the option is not found
   * @throws ExException("E474: Invalid argument: $token") in case the method was called for the [ToggleOption]
   * @throws ExException("E474: Invalid argument: $token") in case the method was called for the [StringOption] and the argument is invalid (does not satisfy the option bounded values)
   * @throws ExException("E521: Number required after =: $token") in case the cast to VimInt is impossible
   */
  fun removeValue(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, value: String, token: String = optionName)

  /**
   * Checks if the toggle option on.
   *
   * Returns false if [optionName] is not a [ToggleOption]
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param token used in exception messages
   * @throws ExException("E518: Unknown option: $token") in case the option is not found
   */
  fun isSet(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, token: String = optionName): Boolean

  /**
   * Checks if the option's value set to default.
   *
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param token used in exception messages
   * @throws ExException("E518: Unknown option: $token") in case the option is not found
   */
  fun isDefault(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, token: String = optionName): Boolean

  /**
   * Resets option's value to default.
   *
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param token used in exception messages
   * @throws ExException("E518: Unknown option: $token") in case the option is not found
   */
  fun resetDefault(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, token: String = optionName)

  /**
   * Resets all options back to default values.
   */
  fun resetAllOptions()

  /**
   * Checks if the option with given optionName is a toggleOption.
   * @param optionName option name or alias
   */
  fun isToggleOption(optionName: String): Boolean

  /**
   * Sets the option on (true).
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param token used in exception messages
   * @throws ExException("E518: Unknown option: $token") in case the option is not found
   * @throws ExException("E474: Invalid argument: $token") in case the option is not a [ToggleOption]
   */
  fun setOption(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, token: String = optionName)

  /**
   * COMPATIBILITY-LAYER: New method added
   * Please see: https://jb.gg/zo8n0r
   */
  fun setOption(scope: Scope, optionName: String, token: String = optionName)

  /**
   * Unsets the option (false).
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param token used in exception messages
   * @throws ExException("E518: Unknown option: $token") in case the option is not found
   * @throws ExException("E474: Invalid argument: $token") in case the option is not a [ToggleOption]
   */
  fun unsetOption(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, token: String = optionName)

  /**
   * Inverts boolean option value true -> false / false -> true.
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param token used in exception messages
   * @throws ExException("E518: Unknown option: $token") in case the option is not found
   * @throws ExException("E474: Invalid argument: $token") in case the option is not a [ToggleOption]
   */
  fun toggleOption(scope: com.maddyhome.idea.vim.options.OptionScope, optionName: String, token: String = optionName)

  /**
   * @return list of all option names
   */
  fun getOptions(): Set<String>

  /**
   * @return list of all option abbreviations
   */
  fun getAbbrevs(): Set<String>

  /**
   * Adds the option.
   * @param option option
   */
  fun addOption(option: Option<out VimDataType>)

  /**
   * Removes the option.
   * @param optionName option name or alias
   */
  fun removeOption(optionName: String)

  /**
   * Adds a listener to the option.
   * @param optionName option name or alias
   * @param listener option listener
   * @param executeOnAdd whether execute listener after the method call or not
   */
  fun addListener(optionName: String, listener: OptionChangeListener<VimDataType>, executeOnAdd: Boolean = false)

  /**
   * Remove the listener from the option.
   * @param optionName option name or alias
   * @param listener option listener
   */
  fun removeListener(optionName: String, listener: OptionChangeListener<VimDataType>)

  /**
   * Get the [Option] by its name or abbreviation
   */
  fun getOptionByNameOrAbbr(key: String): Option<out VimDataType>?

  /**
   * COMPATIBILITY-LAYER: Added this class
   * Please see: https://jb.gg/zo8n0r
   */
  sealed class Scope {
    object GLOBAL : Scope()
    class LOCAL(val editor: VimEditor) : Scope()
  }
}
