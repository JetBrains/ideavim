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
import org.jetbrains.annotations.TestOnly

public interface VimOptionGroup {
  /**
   * Called to initialise the options
   *
   * This function must be idempotent, as it is called each time the plugin is enabled.
   */
  public fun initialiseOptions()

  /**
   * Initialise the local to buffer and local to window options for this editor
   *
   * Local to buffer options are copied from the current global values, while local to window options should be copied
   * from the per-window "global" values of the editor that caused this editor to open. Both of these global values are
   * updated by the `:set` or `:setglobal` commands.
   *
   * Note that global-local options are not copied from the source window. They are global values that are overridden
   * locally, and local values are never copied.
   *
   * TODO: IdeaVim currently does not support per-window "global" values
   *
   * @param editor  The editor to initialise
   * @param sourceEditor  The editor which is opening the new editor. This source editor is used to get the per-window
   *                      "global" values to initialise the new editor. If null, there is no source editor (e.g. all
   *                      editor windows are closed), and the options should be initialised to some other value.
   * @param isSplit True if the new editor is a split view of the source editor
   */
  public fun initialiseLocalOptions(editor: VimEditor, sourceEditor: VimEditor?, isSplit: Boolean)

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
   * Get or create cached, parsed data for the option value effective for the editor
   *
   * The parsed data is created by the given [provider], based on the effective value of the option in the given
   * [editor] (there is no reason to parse global/local data unless it is the effective value). The parsed data is then
   * cached, and the cache is cleared when the effective option value is changed.
   *
   * It is not expected for this function to be used by general purpose use code, but by helper objects that will parse
   * complex options and provide a user facing API for the data. E.g. for `'guicursor'` and `'iskeyword'` options.
   *
   * @param option  The option to return parsed data for
   * @param editor  The editor to get the option value for. This must be specified for local or global-local options
   * @param provider  If the parsed value does not exist, the effective option value is retrieved and passed to the
   *                  provider. The resulting value is cached.
   * @return The cached, parsed option value, ready to be used by code.
   */
  public fun <T : VimDataType, TData : Any> getParsedEffectiveOptionValue(
    option: Option<T>,
    editor: VimEditor?,
    provider: (T) -> TData,
  ): TData

  /**
   * Resets all options for the given editor, including global options, back to default values.
   *
   * In line with `:set all&`, this will reset all option for the given editor. This means resetting global,
   * global-local and local-to-buffer options, which will affect other editors/windows. It resets the local-to-window
   * options for the current editor; this does not affect other editors.
   */
  public fun resetAllOptions(editor: VimEditor)

  /**
   * Resets all options across all editors, to reset state for testing
   *
   * This is required to reset global options set for tests that don't create an editor
   */
  @TestOnly
  public fun resetAllOptionsForTesting()

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

/**
 * Checks a string list option to see if it contains a specific value
 */
public fun VimOptionGroup.hasValue(option: StringListOption, scope: OptionScope, value: String): Boolean {
  val optionValue = getOptionValue(option, scope)
  return option.split(optionValue.asString()).contains(value)
}