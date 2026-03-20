/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Scope that provides access to Vim variables.
 *
 * Example usage:
 * ```kotlin
 * // Lambda style
 * val name = api.variables { get<String>("g:name") }
 *
 * // Direct object style
 * api.variables().set("g:x", 1)
 * ```
 */
@VimApiDsl
interface VariableScope {
  /**
   * Retrieves a variable of the specified type and name.
   * Use the extension function `get<String>("name")`
   */
  fun <T : Any> getVariable(name: String, type: KType): T?

  /**
   * Sets a variable with the specified name and value.
   * Use the extension function `set<String>("name", value)`
   *
   * In Vim, this is equivalent to `let varname = value`.
   */
  fun setVariable(name: String, value: Any, type: KType)
}

/**
 * Retrieves a variable of the specified type and name.
 *
 * Example usage:
 * ```
 * val value: String? = get<String>("g:myVariable")
 * ```
 *
 * @param name The name of the variable to retrieve.
 * @return The variable of type `T` if found, otherwise `null`.
 */
inline fun <reified T : Any> VariableScope.get(name: String): T? {
  val kType: KType = typeOf<T>()
  return getVariable(name, kType)
}

/**
 * Sets a variable with the specified name and value.
 *
 * In Vim, this is equivalent to `let varname = value`.
 *
 * Example usage:
 * ```
 * set<Int>("g:my_var", 42)
 * ```
 *
 * @param name The name of the variable, optionally prefixed with a scope (g:, b:, etc.)
 * @param value The value to set
 */
inline fun <reified T : Any> VariableScope.set(name: String, value: T) {
  val kType: KType = typeOf<T>()
  setVariable(name, value, kType)
}
