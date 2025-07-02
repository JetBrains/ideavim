/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import kotlin.reflect.KType
import kotlin.reflect.typeOf

abstract class OptionScope() {
  protected abstract fun <T> getOptionValueInternal(name: String, type: KType): T?

  protected abstract fun <T> setOptionInternal(name: String, value: T, type: KType, scope: String): Boolean

  @PublishedApi
  internal fun <T : Any> getOptionValue(name: String, type: KType): T? = getOptionValueInternal(name, type)

  @PublishedApi
  internal fun <T> setGlobal(name: String, value: T, type: KType): Boolean =
    setOptionInternal(name, value, type, "global")

  @PublishedApi
  internal fun <T> setLocal(name: String, value: T, type: KType): Boolean =
    setOptionInternal(name, value, type, "local")

  @PublishedApi
  internal fun <T> set(name: String, value: T, type: KType): Boolean =
    setOptionInternal(name, value, type, "effective")

  /**
   * Gets the value of an option with the specified type.
   *
   * In Vim, options can be accessed with the `&` prefix.
   * Example: `&ignorecase` returns the value of the 'ignorecase' option.
   *
   * @param name The name of the option
   * @return The value of the option, or null if the option doesn't exist or isn't of the specified type
   */
  inline fun <reified T> getOptionValue(name: String): T? {
    val kType: KType = typeOf<T>()
    return getOptionValue(name, kType)
  }

  /**
   * Sets the global value of an option with the specified type.
   *
   * In Vim, this is equivalent to `:setglobal option=value`.
   * Example: `:setglobal ignorecase` or `let &g:ignorecase = 1`
   *
   * @param name The name of the option
   * @param value The value to set
   * @return True if the option was set successfully, false otherwise
   */
  inline fun <reified T> setGlobal(name: String, value: T): Boolean {
    val kType: KType = typeOf<T>()
    return setGlobal(name, value, kType)
  }

  /**
   * Sets the local value of an option with the specified type.
   *
   * In Vim, this is equivalent to `:setlocal option=value`.
   * Example: `:setlocal ignorecase` or `let &l:ignorecase = 1`
   *
   * @param name The name of the option
   * @param value The value to set
   * @return True if the option was set successfully, false otherwise
   */
  inline fun <reified T> setLocal(name: String, value: T): Boolean {
    val kType: KType = typeOf<T>()
    return setLocal(name, value, kType)
  }

  /**
   * Sets the effective value of an option with the specified type.
   *
   * In Vim, this is equivalent to `:set option=value`.
   * Example: `:set ignorecase` or `let &ignorecase = 1`
   *
   * @param name The name of the option
   * @param value The value to set
   * @return True if the option was set successfully, false otherwise
   */
  inline fun <reified T> set(name: String, value: T): Boolean {
    val kType: KType = typeOf<T>()
    return set(name, value, kType)
  }

  /**
   * Resets an option to its default value.
   *
   * In Vim, this is equivalent to `:set option&`.
   * Example: `:set ignorecase&` resets the 'ignorecase' option to its default value.
   *
   * @param name The name of the option
   * @return True if the option was reset successfully, false otherwise
   */
  abstract fun resetOptionToDefault(name: String): Boolean
}