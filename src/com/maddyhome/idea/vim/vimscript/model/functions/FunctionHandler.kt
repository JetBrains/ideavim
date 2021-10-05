/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.vimscript.model.functions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope

abstract class FunctionHandler {

  abstract val name: String
  open val scope: Scope? = null
  abstract val minimumNumberOfArguments: Int?
  abstract val maximumNumberOfArguments: Int?
  var ranges: Ranges? = null

  protected abstract fun doFunction(argumentValues: List<Expression>, editor: Editor, context: DataContext, parent: Executable): VimDataType

  fun executeFunction(arguments: List<Expression>, editor: Editor, context: DataContext, parent: Executable): VimDataType {
    checkFunctionCall(arguments)
    val result = doFunction(arguments, editor, context, parent)
    ranges = null
    return result
  }

  private fun checkFunctionCall(arguments: List<Expression>) {
    if (minimumNumberOfArguments != null && arguments.size < minimumNumberOfArguments!!) {
      throw ExException("E119: Not enough arguments for function: $name")
    }
    if (maximumNumberOfArguments != null && arguments.size > maximumNumberOfArguments!!) {
      throw ExException("E118: Too many arguments for function: $name")
    }
  }
}
