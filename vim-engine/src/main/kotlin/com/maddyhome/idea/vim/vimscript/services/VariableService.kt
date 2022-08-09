/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import org.jetbrains.annotations.TestOnly

/**
 * COMPATIBILITY-LAYER: Renamed from VimVariableService
 * Please see: https://jb.gg/zo8n0r
 */
interface VariableService {
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
  fun storeVariable(variable: Variable, value: VimDataType, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext)

  /**
   * Get global scope variable value.
   * @param name variable name
   */
  fun getGlobalVariableValue(name: String): VimDataType?

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
  fun getNullableVariableValue(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType?

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
  fun getNonNullVariableValue(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType

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
  fun isVariableLocked(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): Boolean

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
  fun lockVariable(variable: Variable, depth: Int, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext)

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
  fun unlockVariable(variable: Variable, depth: Int, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext)

  fun getGlobalVariables(): Map<String, VimDataType>

  /**
   * Clears all global variables
   */
  @TestOnly
  fun clear()
}
