/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.Color
import com.intellij.vim.api.Mode
import com.intellij.vim.api.TextSelectionType
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@VimPluginDsl
interface VimScope {
  var mode: Mode
  fun getSelectionTypeForCurrentMode(): TextSelectionType?

  /**
   * Retrieves a VimScript variable and converts it to the specified Kotlin type.
   *
   * Supports data types: Int, String, Double, List<T>, Map<String, T> and nested combinations.
   *
   * @param name The name of the variable to retrieve (scope prefix (g:, b:) followed by name)
   * @param type The Kotlin type to convert the variable to
   * @return The variable value converted to the specified type
   * @throws IllegalArgumentException if the variable doesn't exist, there's a type mismatch, or unsupported type is requested
   */
  fun <T : Any> getVariable(name: String, type: KType): T?
  fun exportOperatorFunction(name: String, function: VimScope.() -> Boolean)
  fun setOperatorFunction(name: String)
  fun normal(command: String)

  fun editor(block: EditorScope.() -> Unit)
  fun mappings(block: MappingScope.() -> Unit)
  fun listeners(block: ListenersScope.() -> Unit)

  /**
   * String in format: rgba(0,0,0,0)
   *
   * Throws IllegalArgumentException if string is not in correct format
   */
  fun parseRgbaColor(rgbaString: String): Color?
}

/**
 * Convenient extension function for retrieving a VimScript variable with type inference.
 *
 * @param name The name of the variable to retrieve, including scope prefix
 * @return The variable value converted to the specified type
 * @throws IllegalArgumentException if the variable doesn't exist, there's a type mismatch, or unsupported type is requested
 * @see getVariable
 */
inline fun <reified T : Any> VimScope.getVariable(name: String): T? {
  val kType: KType = typeOf<T>()
  return getVariable(name, kType)
}
