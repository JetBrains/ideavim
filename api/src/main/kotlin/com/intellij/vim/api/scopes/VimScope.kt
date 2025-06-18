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
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@VimPluginDsl
abstract class VimScope {
  abstract var mode: Mode

  protected abstract fun <T : Any> getVariableInternal(name: String, type: KType): T?

  @PublishedApi
  internal fun <T : Any> getVariable(name: String, type: KType): T? = getVariableInternal(name, type)

  inline fun <reified T : Any> getVariable(name: String): T? {
    val kType: KType = typeOf<T>()
    return getVariable(name, kType)
  }

  abstract fun exportOperatorFunction(name: String, function: VimScope.() -> Boolean)
  abstract fun setOperatorFunction(name: String)
  abstract fun normal(command: String)

  abstract fun editor(block: EditorScope.() -> Unit)
  abstract fun mappings(block: MappingScope.() -> Unit)
  abstract fun listeners(block: ListenersScope.() -> Unit)

  /**
   * String in format: rgba(0,0,0,0)
   *
   * Throws IllegalArgumentException if string is not in correct format
   */
  abstract fun parseRgbaColor(rgbaString: String): Color?
}
