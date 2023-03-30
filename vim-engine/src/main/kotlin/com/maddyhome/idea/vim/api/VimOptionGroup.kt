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
import com.maddyhome.idea.vim.options.StringListOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

public interface VimOptionGroup {
  /**
   * Called to initialise the options
   *
   * This function must be idempotent, as it is called each time the plugin is enabled.
   */
  public fun initialiseOptions()

  /**
   * Get the [Option] by its name or abbreviation
   */
  public fun getOption(key: String): Option<VimDataType>?

  /**
   * @return list of all options
   */
  public fun getAllOptions(): Set<Option<VimDataType>>

  /**
   * Get the value for the option in the given scope
   */
  public fun <T : VimDataType> getOptionValue(option: Option<T>, scope: OptionScope): T

  /**
   * Set the value for the option in the given scope
   */
  public fun <T : VimDataType> setOptionValue(option: Option<T>, scope: OptionScope, value: T)

  /**
   * Resets all options back to default values.
   */
  public fun resetAllOptions()

  /**
   * Adds the option.
   *
   * Note that this function accepts a covariant version of [Option] so it can accept derived instances that are
   * specialised by a type derived from [VimDataType].
   *
   * @param option option
   */
  public fun addOption(option: Option<out VimDataType>)

  /**
   * Removes the option.
   * @param optionName option name or alias
   */
  public fun removeOption(optionName: String)

  /**
   * Adds a listener to the option.
   * @param option the option
   * @param listener option listener
   * @param executeOnAdd whether execute listener after the method call or not
   */
  public fun <T : VimDataType> addListener(option: Option<T>,
                                           listener: OptionChangeListener<T>,
                                           executeOnAdd: Boolean = false)

  /**
   * Remove the listener from the option.
   * @param option the option
   * @param listener option listener
   */
  public fun <T : VimDataType> removeListener(option: Option<T>, listener: OptionChangeListener<T>)

  /**
   * Override the original default value of the option with an implementation specific value
   *
   * This is added specifically for `'clipboard'` to support the `ideaput` value in the IntelliJ implementation.
   * This function should be used with care!
   */
  public fun <T : VimDataType> overrideDefaultValue(option: Option<T>, newDefaultValue: T)

  /**
   * Return an accessor for options that only have a global value
   */
  public fun getGlobalOptions(): GlobalOptions

  /**
   * Return an accessor for the effective value of local options
   */
  public fun getEffectiveOptions(editor: VimEditor): EffectiveOptions
}

/**
 * Checks if option is set to its default value
 */
public fun <T: VimDataType> VimOptionGroup.isDefaultValue(option: Option<T>, scope: OptionScope): Boolean =
  getOptionValue(option, scope) == option.defaultValue

/**
 * Resets the option back to its default value
 */
public fun <T: VimDataType> VimOptionGroup.resetDefaultValue(option: Option<T>, scope: OptionScope) {
  setOptionValue(option, scope, option.defaultValue)
}

/**
 * Splits a string list option into flags, or returns a list with a single string value
 *
 * E.g. the `fileencodings` option with value "ucs-bom,utf-8,default,latin1" will result listOf("ucs-bom", "utf-8", "default", "latin1")
 */
public fun VimOptionGroup.getStringListValues(option: StringListOption, scope: OptionScope): List<String> {
  return option.split(getOptionValue(option, scope).asString())
}

/**
 * Sets the toggle option on
 */
public fun VimOptionGroup.setToggleOption(option: ToggleOption, scope: OptionScope) {
  setOptionValue(option, scope, VimInt.ONE)
}

/**
 * Unsets a toggle option
 */
public fun VimOptionGroup.unsetToggleOption(option: ToggleOption, scope: OptionScope) {
  setOptionValue(option, scope, VimInt.ZERO)
}

/**
 * Inverts toggle option value, setting it on if off, or off if on.
 */
public fun VimOptionGroup.invertToggleOption(option: ToggleOption, scope: OptionScope) {
  val optionValue = getOptionValue(option, scope)
  setOptionValue(option, scope, if (optionValue.asBoolean()) VimInt.ZERO else VimInt.ONE)
}
