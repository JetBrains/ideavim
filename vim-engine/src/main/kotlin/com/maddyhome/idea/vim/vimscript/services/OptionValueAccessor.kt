/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.maddyhome.idea.vim.options.OptionScope

/**
 * API for accessing option values in the given scope
 *
 * This class is intended to be used by code that needs to check option values to modify behaviour. Code that needs to
 * administer options should use [OptionService] (e.g. `:set`, `:setlocal`, `:setglobal`).
 *
 * All functions assume that the option exists, and that the calling code knows what type to expect. Trying to retrieve
 * a non-existent option, or calling the wrong accessor will lead to exceptions or incorrect behaviour.
 */
class OptionValueAccessor(private val optionService: OptionService, val scope: OptionScope) {
  /** Gets the loosely typed option value */
  fun getValue(optionName: String) = optionService.getOptionValue(scope, optionName)

  /** Gets the option value as an integer */
  fun getIntValue(optionName: String) = getValue(optionName).toVimNumber().value

  /** Gets the option value as a string */
  fun getStringValue(optionName: String) = getValue(optionName).asString()

  /**
   * Gets the option value as a string list
   *
   * @see hasValue
   */
  fun getStringListValues(optionName: String) = optionService.getValues(scope, optionName)!!

  /** Checks if a string list option contains a value, or if a simple string value matches the given value
   *
   * If the option is a string option, the given value must match the entire string
   */
  fun hasValue(optionName: String, value: String) = optionService.contains(scope, optionName, value)

  /**
   * Checks the option value is set/true
   *
   * The option is most likely a toggle option, but this is not required.
   */
  fun isSet(optionName: String) = getValue(optionName).asBoolean()
}
