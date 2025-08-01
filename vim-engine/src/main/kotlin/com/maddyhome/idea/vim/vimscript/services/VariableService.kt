/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.TestOnly
import kotlin.reflect.KType

/**
 * COMPATIBILITY-LAYER: Renamed from VimVariableService
 * Please see: https://jb.gg/zo8n0r
 */
interface VariableService {
  /**
   * Stores variable.
   *
   * The `v:` scope currently is not supported.
   * @param variable variable to store, if its scope is null, the default scope for vimContext will be chosen
   * @param value variable value
   * @param editor editor
   * @param context execution context
   * @param vimContext vim context
   * @throws ExException("The 'v:' scope is not implemented yet :(")
   */
  fun storeVariable(
    variable: Variable,
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  )

  /**
   * Stores global scope variable value.
   * @param name variable name
   * @param value variable value
   */
  fun storeGlobalVariable(name: String, value: VimDataType)

  /**
   * Gets global scope variable value.
   * @param name variable name
   */
  fun getGlobalVariableValue(name: String): VimDataType?

  /**
   * Gets variable value.
   *
   * The `v:` scope currently is not supported.
   * @param variable variable, if its scope is null, the default scope for vimContext will be chosen
   * @param editor editor
   * @param context execution context
   * @param vimContext vim context
   * @throws ExException("The 'v:' scope is not implemented yet :(")
   * @throws ExException if a required parameter is null for the specific scope
   * @throws ExException("VimLContext is required to determine the default variable scope") if variable.scope is null and vimContext is null
   * @throws ExException("Cannot extract variable name without editor, context, and vimContext") if the variable name cannot be extracted without the parameters
   */
  fun getNullableVariableValue(
    variable: Variable,
    editor: VimEditor?,
    context: ExecutionContext?,
    vimContext: VimLContext?,
  ): VimDataType?

  /**
   * Gets variable value.
   *
   * The `v:` scope currently is not supported.
   * @param variable variable, if its scope is null, the default scope for vimContext will be chosen
   * @param editor editor
   * @param context execution context
   * @param vimContext vim context
   * @throws ExException("The 'v:' scope is not implemented yet :(")
   * @throws ExException("E121: Undefined variable: ${scope}:${name}")
   */
  fun getNonNullVariableValue(
    variable: Variable,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType

  /**
   * Checks if the variable is locked.
   *
   * Returns false if the variable does not exist.
   *
   * See `:h lockvar`.
   * @param variable variable, if its scope is null, the default scope for vimContext will be chosen
   * @param editor editor
   * @param context execution context
   * @param vimContext vim context
   */
  fun isVariableLocked(
    variable: Variable,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): Boolean

  /**
   * Locks variable.
   *
   * See `:h lockvar`.
   * @param variable variable, if its scope is null, the default scope for vimContext will be chosen
   * @param depth lock depth
   * @param editor editor
   * @param context execution context
   * @param vimContext vim context
   */
  fun lockVariable(
    variable: Variable,
    depth: Int,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  )

  /**
   * Unlocks variable.
   *
   * See `:h lockvar`.
   * @param variable variable, if its scope is null, the default scope for vimContext will be chosen
   * @param depth lock depth
   * @param editor editor
   * @param context execution context
   * @param vimContext vim context
   */
  fun unlockVariable(
    variable: Variable,
    depth: Int,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  )

  fun getGlobalVariables(): Map<String, VimDataType>

  /**
   * Converts a VimDataType to a Kotlin type.
   *
   * @param vimDataType The VimDataType to convert
   * @param type The target Kotlin type
   * @return The converted value
   * @throws IllegalArgumentException if the conversion is not possible
   */
  fun <T : Any> convertToKotlinType(vimDataType: VimDataType, type: KType): T

  /**
   * Converts a Kotlin value to a VimDataType.
   *
   * @param value The Kotlin value to convert
   * @param type The Kotlin type information
   * @return The converted VimDataType
   * @throws IllegalArgumentException if the conversion is not possible
   */
  fun convertToVimDataType(value: Any, type: KType): VimDataType

  /**
   * Clears all global variables.
   */
  @TestOnly
  fun clear()

  @Internal
  fun getVimVariable(name: String): VimDataType?

  @Internal
  fun storeVimVariable(name: String, value: VimDataType)
}
