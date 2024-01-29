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

/**
 * COMPATIBILITY-LAYER: Renamed from VimVariableService
 * Please see: https://jb.gg/zo8n0r
 */
public interface VariableService {
  /**
   * Stores variable.
   *
   * The `v:` scope currently is not supported.
   * @param variable variable to store, if it's scope is null, the default scope for vimContext will be chosen
   * @param value variable value
   * @param editor editor
   * @param context execution context
   * @param vimContext vim context
   * @throws ExException("The 'v:' scope is not implemented yet :(")
   */
  public fun storeVariable(variable: Variable, value: VimDataType, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext)

  /**
   * Get global scope variable value.
   * @param name variable name
   * @param value variable value
   */
  public fun storeGlobalVariable(name: String, value: VimDataType)

  /**
   * Get global scope variable value.
   * @param name variable name
   */
  public fun getGlobalVariableValue(name: String): VimDataType?

  /**
   * Gets variable value
   *
   * The `v:` scope currently is not supported
   * @param variable variable, if it's scope is null, the default scope for vimContext will be chosen
   * @param editor editor
   * @param context execution context
   * @param vimContext vim context
   * @throws ExException("The 'v:' scope is not implemented yet :(")
   */
  public fun getNullableVariableValue(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType?

  /**
   * Gets variable value.
   *
   * The `v:` scope currently is not supported.
   * @param variable variable, if it's scope is null, the default scope for vimContext will be chosen
   * @param editor editor
   * @param context execution context
   * @param vimContext vim context
   * @throws ExException("The 'v:' scope is not implemented yet :(")
   * @throws ExException("E121: Undefined variable: ${scope}:${name}")
   */
  public fun getNonNullVariableValue(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType

  /**
   * Checks if the variable locked.
   *
   * Returns false if the variable does not exist.
   *
   * See `:h lockvar`.
   * @param variable variable, if it's scope is null, the default scope for vimContext will be chosen
   * @param editor editor
   * @param context execution context
   * @param vimContext vim context
   */
  public fun isVariableLocked(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): Boolean

  /**
   * Locks variable.
   *
   * See `:h lockvar`.
   * @param variable variable, if it's scope is null, the default scope for vimContext will be chosen
   * @param depth lock depth
   * @param editor editor
   * @param context execution context
   * @param vimContext vim context
   */
  public fun lockVariable(variable: Variable, depth: Int, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext)

  /**
   * Unlocks variable.
   *
   * See `:h lockvar`.
   * @param variable variable, if it's scope is null, the default scope for vimContext will be chosen
   * @param depth lock depth
   * @param editor editor
   * @param context execution context
   * @param vimContext vim context
   */
  public fun unlockVariable(variable: Variable, depth: Int, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext)

  public fun getGlobalVariables(): Map<String, VimDataType>

  /**
   * Clears all global variables
   */
  @TestOnly
  public fun clear()

  @Internal
  public fun getVimVariable(name: String): VimDataType?
  @Internal
  public fun storeVimVariable(name: String, value: VimDataType)
}
