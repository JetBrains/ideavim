/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api

import com.intellij.vim.api.scopes.CommandScope
import com.intellij.vim.api.scopes.MappingScope
import com.intellij.vim.api.scopes.TextObjectScope
import com.intellij.vim.api.scopes.VariableScope
import com.intellij.vim.api.scopes.get
import org.jetbrains.annotations.ApiStatus

/**
 * Restricted API available during plugin initialization.
 *
 * During `init()`, there is no editor context yet, so only registration methods
 * (mappings, text objects, variables, commands) are exposed.
 * Editor operations and other runtime-only features are intentionally omitted.
 *
 * This is a delegation wrapper around [VimApi] — it exposes only the init-safe subset.
 */
@ApiStatus.Experimental
class VimInitApi(private val delegate: VimApi) {
  fun <T> variables(block: VariableScope.() -> T): T = delegate.variables(block)

  fun <T> commands(block: CommandScope.() -> T): T = delegate.commands(block)

  fun <T> mappings(block: MappingScope.() -> T): T = delegate.mappings(block)

  fun <T> textObjects(block: TextObjectScope.() -> T): T = delegate.textObjects(block)
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
  return variables { get(name) }
}
