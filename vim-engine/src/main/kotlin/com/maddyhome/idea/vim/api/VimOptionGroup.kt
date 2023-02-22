/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionChangeListener
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.OptionValueAccessor
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.options.appendValue
import com.maddyhome.idea.vim.options.removeValue
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

interface VimOptionGroup {
  /**
   * Get the [Option] by its name or abbreviation
   */
  fun getOption(key: String): Option<out VimDataType>?

  /**
   * @return list of all options
   */
  fun getAllOptions(): Set<Option<out VimDataType>>

  /**
   * Get the value for the option in the given scope
   */
  fun getOptionValue(option: Option<out VimDataType>, scope: OptionScope): VimDataType

  /**
   * Set the value for the option in the given scope
   */
  fun setOptionValue(option: Option<out VimDataType>, scope: OptionScope, value: VimDataType)

  /**
   * Resets all options back to default values.
   */
  fun resetAllOptions()

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
 * Checks if option is set to its default value
 */
fun VimOptionGroup.isDefaultValue(option: Option<out VimDataType>, scope: OptionScope) =
  getOptionValue(option, scope) == option.defaultValue

/**
 * Resets the option back to its default value
 */
fun VimOptionGroup.resetDefaultValue(option: Option<out VimDataType>, scope: OptionScope) {
  setOptionValue(option, scope, option.defaultValue)
}

/**
 * Checks if the given string option matches the value, or a string list contains the value
 */
fun VimOptionGroup.hasValue(option: StringOption, scope: OptionScope, value: String) =
  value in option.split(getOptionValue(option, scope).asString())

/**
 * Splits a string list option into flags, or returns a list with a single string value
 *
 * E.g. the `fileencodings` option with value "ucs-bom,utf-8,default,latin1" will result listOf("ucs-bom", "utf-8", "default", "latin1")
 */
fun VimOptionGroup.getStringListValues(option: StringOption, scope: OptionScope): List<String> {
  return option.split(getOptionValue(option, scope).asString())
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


/**
 * Convenience function to append a value to a known global option
 *
 * There is no validation - the option is assumed to exist, be a string list option, and the passed value is assumed to
 * be valid.
 */
fun VimOptionGroup.unsafeAppendGlobalKnownOptionValue(optionName: String, value: String) {
  val option = getOption(optionName)!!
  val existingValue = getOptionValue(option, OptionScope.GLOBAL)
  val newValue = option.appendValue(existingValue, VimString(value))!!
  setOptionValue(option, OptionScope.GLOBAL, newValue)
}

/**
 * Convenience function to remove a value to a known global option
 *
 * There is no validation - the option is assumed to exist, be a string list option, and the passed value is assumed to
 * be valid.
 */
fun VimOptionGroup.unsafeRemoveGlobalKnownOptionValue(optionName: String, value: String) {
  val option = getOption(optionName)!!
  val existingValue = getOptionValue(option, OptionScope.GLOBAL)
  val newValue = option.removeValue(existingValue, VimString(value))!!
  setOptionValue(option, OptionScope.GLOBAL, newValue)
}
