/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionChangeListener
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.OptionValueAccessor
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

interface VimOptionGroup {

  /**
   * Get the value for the option in the given scope
   */
  fun getOptionValue(option: Option<out VimDataType>, scope: OptionScope): VimDataType

  /**
   * Set the value for the option in the given scope
   */
  fun setOptionValue(option: Option<out VimDataType>, scope: OptionScope, value: VimDataType)



  /**
   * Checks if the [value] is contained in string option.
   *
   * Returns false if there is no option with the given optionName, or it's type is different from string.
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @param value option value
   */
  fun contains(scope: OptionScope, optionName: String, value: String): Boolean

  /**
   * Splits a string option into flags
   *
   * e.g. the `fileencodings` option with value "ucs-bom,utf-8,default,latin1" will result listOf("ucs-bom", "utf-8", "default", "latin1")
   *
   * returns null if there is no option with the given optionName, or its type is different from string.
   * @param scope global/local option scope
   * @param optionName option name or alias
   */
  fun getValues(scope: OptionScope, optionName: String): List<String>?

  /**
   * Checks if the option's value set to default.
   *
   * @param scope global/local option scope
   * @param optionName option name or alias
   * @throws ExException("E518: Unknown option: $optionName") in case the option is not found
   */
  fun isDefault(scope: OptionScope, optionName: String): Boolean


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
   * @return list of all option names
   */
  fun getOptions(): Set<String>

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
  fun getOption(key: String): Option<out VimDataType>?

  /**
   * Return an accessor class to easily retrieve options values
   *
   * Note that passing `null` as an editor means that you're only interested in global options - NOT global values of
   * local to buffer or local to window or global-local options! For that, use [getOptionValue].
   *
   * @param editor The editor to use to retrieve local option values. If `null`, then only global values are available
   * @return An instance of [OptionValueAccessor] to provide easy API to get option values
   */
  fun getValueAccessor(editor: VimEditor?): OptionValueAccessor
}

/**
 * Resets the option back to its default value
 */
fun VimOptionGroup.resetDefaultValue(option: Option<out VimDataType>, scope: OptionScope) {
  setOptionValue(option, scope, option.defaultValue)
}

/**
 * Sets the toggle option on
 */
fun VimOptionGroup.setToggleOption(option: ToggleOption, scope: OptionScope) {
  setOptionValue(option, scope, VimInt.ONE)
}

/**
 * Unsets a toggle option
 */
fun VimOptionGroup.unsetToggleOption(option: ToggleOption, scope: OptionScope) {
  setOptionValue(option, scope, VimInt.ZERO)
}

/**
 * Inverts toggle option value, setting it on if off, or off if on.
 */
fun VimOptionGroup.invertToggleOption(option: ToggleOption, scope: OptionScope) {
  val optionValue = getOptionValue(option, scope)
  setOptionValue(option, scope, if (optionValue.asBoolean()) VimInt.ZERO else VimInt.ONE)
}
