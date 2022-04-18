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
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable

interface VariableService {

  fun isVariableLocked(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): Boolean

  fun lockVariable(variable: Variable, depth: Int, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext)

  fun unlockVariable(variable: Variable, depth: Int, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext)

  fun storeVariable(variable: Variable, value: VimDataType, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext)

  // todo replace with one method after Result class
  fun getGlobalVariableValue(name: String): VimDataType?
  fun getNullableVariableValue(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType?
  fun getNonNullVariableValue(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType
}
