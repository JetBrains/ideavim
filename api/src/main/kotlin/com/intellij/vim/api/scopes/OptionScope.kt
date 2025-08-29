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

/**
 * Scope that provides functions for working with options.
 */
@VimApiDsl
interface OptionScope {
  /**
   * Gets the value of an option with the specified type.
   * 
   * **Note:** Prefer using the extension function `get<T>(name)` instead of calling this directly,
   * as it provides better type safety and cleaner syntax through reified type parameters.
   *
   * Example of preferred usage:
   * ```kotlin
   * myVimApi.option {
   *   val ignoreCase = get<Boolean>("ignorecase")
   *   val history = get<Int>("history")
   *   val clipboard = get<String>("clipboard")
   * }
   * ```
   *
   * @param name The name of the option
   * @param type The KType of the option value
   * @return The value of the option
   * @throws IllegalArgumentException if the type is wrong or the option doesn't exist
   */
  fun <T> getOptionValue(name: String, type: KType): T

  /**
   * Sets an option value with the specified scope.
   * 
   * **Note:** Prefer using the extension functions `set<T>(name, value)`, `setGlobal<T>(name, value)`, 
   * or `setLocal<T>(name, value)` instead of calling this directly, as they provide better type safety 
   * and cleaner syntax through reified type parameters.
   *
   * Example of preferred usage:
   * ```kotlin
   * myVimApi.option {
   *   set("ignorecase", true)        // Effective scope
   *   setGlobal("number", 42)         // Global scope
   *   setLocal("tabstop", 4)          // Local scope
   * }
   * ```
   *
   * @param name The name of the option
   * @param value The value to set
   * @param type The KType of the option value
   * @param scope The scope to set the option in ("global", "local", or "effective")
   * @throws IllegalArgumentException if the option doesn't exist or the type is wrong
   */
  fun <T> setOption(name: String, value: T, type: KType, scope: String)

  /**
   * Resets an option to its default value.
   *
   * In Vim, this is equivalent to `:set option&`.
   * Example: `:set ignorecase&` resets the 'ignorecase' option to its default value.
   *
   * @param name The name of the option
   *
   * @throws IllegalArgumentException if the option doesn't exist
   */
  fun reset(name: String)

  /**
   * Extension function to split a comma-separated option value into a list.
   * This is useful for processing list options like virtualedit, whichwrap, etc.
   *
   * Example:
   * ```kotlin
   * myVimApi.option {
   *   val values = get<String>("virtualedit")?.split() ?: emptyList()
   *   // "block,all" → ["block", "all"]
   *   // "" → [""]
   *   // "all" → ["all"]
   * }
   * ```
   */
  fun String.split(): List<String> = split(",")
}

/**
 * Gets the value of an option with the specified type.
 *
 * In Vim, options can be accessed with the `&` prefix.
 * Example: `&ignorecase` returns the value of the 'ignorecase' option.
 *
 * @param name The name of the option
 * @return The value of the option
 * @throws IllegalArgumentException if the type is wrong or the option doesn't exist
 */
inline fun <reified T> OptionScope.get(name: String): T {
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
 *
 * @throws IllegalArgumentException if the option doesn't exist or the type is wrong
 */
inline fun <reified T> OptionScope.setGlobal(name: String, value: T) {
  val kType: KType = typeOf<T>()
  setOption(name, value, kType, "global")
}

/**
 * Sets the local value of an option with the specified type.
 *
 * In Vim, this is equivalent to `:setlocal option=value`.
 * Example: `:setlocal ignorecase` or `let &l:ignorecase = 1`
 *
 * @param name The name of the option
 * @param value The value to set
 *
 * @throws IllegalArgumentException if the option doesn't exist or the type is wrong
 */
inline fun <reified T> OptionScope.setLocal(name: String, value: T) {
  val kType: KType = typeOf<T>()
  setOption(name, value, kType, "local")
}

/**
 * Sets the effective value of an option with the specified type.
 *
 * In Vim, this is equivalent to `:set option=value`.
 * Example: `:set ignorecase` or `let &ignorecase = 1`
 *
 * @param name The name of the option
 * @param value The value to set
 *
 * @throws IllegalArgumentException if the option doesn't exist or the type is wrong
 */
inline fun <reified T> OptionScope.set(name: String, value: T) {
  val kType: KType = typeOf<T>()
  setOption(name, value, kType, "effective")
}

/**
 * Toggles a boolean option value.
 *
 * Example:
 * ```kotlin
 * myVimApi.option {
 *   toggle("ignorecase")  // true → false, false → true
 * }
 * ```
 *
 * @param name The name of the boolean option to toggle
 */
fun OptionScope.toggle(name: String) {
  val current = get<Boolean>(name)
  set(name, !current)
}

/**
 * Appends values to a comma-separated list option.
 * This is equivalent to Vim's += operator for string options.
 * Duplicate values are not added.
 *
 * Example:
 * ```kotlin
 * myVimApi.option {
 *   append("virtualedit", "block")  // "" → "block"
 *   append("virtualedit", "onemore")  // "block" → "block,onemore"
 *   append("virtualedit", "block")  // "block,onemore" → "block,onemore" (no change)
 * }
 * ```
 *
 * @param name The name of the list option
 * @param values The values to append (duplicates will be ignored)
 */
fun OptionScope.append(name: String, vararg values: String) {
  val currentList = get<String>(name).split()
  val valuesToAdd = values.filterNot { it in currentList }
  val newList = currentList + valuesToAdd
  set(name, newList.joinToString(","))
}

/**
 * Prepends values to a comma-separated list option.
 * This is equivalent to Vim's ^= operator for string options.
 * Duplicate values are not added.
 *
 * Example:
 * ```kotlin
 * myVimApi.option {
 *   prepend("virtualedit", "block")  // "all" → "block,all"
 *   prepend("virtualedit", "onemore")  // "block,all" → "onemore,block,all"
 *   prepend("virtualedit", "all")  // "onemore,block,all" → "onemore,block,all" (no change)
 * }
 * ```
 *
 * @param name The name of the list option
 * @param values The values to prepend (duplicates will be ignored)
 */
fun OptionScope.prepend(name: String, vararg values: String) {
  val currentList = get<String>(name).split()
  val valuesToAdd = values.filterNot { it in currentList }
  val newList = valuesToAdd + currentList
  set(name, newList.joinToString(","))
}

/**
 * Removes values from a comma-separated list option.
 * This is equivalent to Vim's -= operator for string options.
 *
 * Example:
 * ```kotlin
 * myVimApi.option {
 *   remove("virtualedit", "block")  // "block,all" → "all"
 *   remove("virtualedit", "all")  // "all" → ""
 * }
 * ```
 *
 * @param name The name of the list option
 * @param values The values to remove
 */
fun OptionScope.remove(name: String, vararg values: String) {
  val currentList = get<String>(name).split()
  val newList = currentList.filterNot { it in values }
  set(name, newList.joinToString(","))
}