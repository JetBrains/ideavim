/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

import com.intellij.vim.api.scopes.MappingScope
import com.intellij.vim.api.scopes.TextObjectScope
import org.jetbrains.annotations.ApiStatus
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Restricted API available during plugin initialization.
 *
 * During `init()`, there is no editor context yet, so only registration methods
 * (mappings, text objects, variables, operator functions) are exposed.
 * Editor operations and other runtime-only features are intentionally omitted.
 *
 * This is a delegation wrapper around [VimApi] — it exposes only the init-safe subset.
 */
@ApiStatus.Experimental
class VimInitApi(private val delegate: VimApi) {
  fun <T : Any> getVariable(name: String, type: KType): T? = delegate.getVariable(name, type)

  fun mappings(block: MappingScope.() -> Unit = {}): MappingScope = delegate.mappings(block)

  fun textObjects(block: TextObjectScope.() -> Unit = {}): TextObjectScope = delegate.textObjects(block)

  fun exportOperatorFunction(name: String, function: suspend VimApi.() -> Boolean) =
    delegate.exportOperatorFunction(name, function)

  fun command(command: String, block: suspend VimApi.(commandText: String, startLine: Int, endLine: Int) -> Unit) =
    delegate.command(command, block)
}

/**
 * Retrieves a variable of the specified type and name.
 *
 * Example usage:
 * ```
 * val value: String? = getVariable<String>("myVariable")
 * ```
 *
 * @param name The name of the variable to retrieve.
 * @return The variable of type `T` if found, otherwise `null`.
 */
inline fun <reified T : Any> VimInitApi.getVariable(name: String): T? {
  val kType: KType = typeOf<T>()
  return getVariable(name, kType)
}
