/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

import com.maddyhome.idea.vim.api.VimOptionGroup
import com.maddyhome.idea.vim.api.getStringListValues
import com.maddyhome.idea.vim.api.hasValue
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.services.OptionService

/**
 * API for accessing option values in the given scope
 *
 * This class is intended to be used by code that needs to check option values to modify behaviour. Code that needs to
 * administer options should use [OptionService] (e.g. `:set`, `:setlocal`, `:setglobal`).
 *
 * All functions assume that the option exists, and that the calling code knows what type to expect. Trying to retrieve
 * a non-existent option, or calling the wrong accessor will lead to exceptions or incorrect behaviour.
 */
public class OptionValueAccessor(private val optionGroup: VimOptionGroup, public val scope: OptionScope) {
  /** Gets the loosely typed option value */
  public fun getValue(option: Option<out VimDataType>): VimDataType = optionGroup.getOptionValue(option, scope)

  /** Gets the option value as an integer */
  public fun getIntValue(option: NumberOption): Int = getValue(option).toVimNumber().value

  /** Gets the option value as a string */
  public fun getStringValue(option: StringOption): String = getValue(option).asString()

  /**
   * Gets the option value as a string list
   *
   * @see hasValue
   */
  public fun getStringListValues(option: StringOption): List<String> = optionGroup.getStringListValues(option, scope)

  /** Checks if a string list option contains a value, or if a simple string value matches the given value
   *
   * If the option is a string option, the given value must match the entire string
   */
  public fun hasValue(option: StringOption, value: String): Boolean = optionGroup.hasValue(option, scope, value)

  /**
   * Checks the option value is set/true
   *
   * The option is most likely a toggle option, but this is not required.
   */
  public fun isSet(option: ToggleOption): Boolean = getValue(option).asBoolean()
}
