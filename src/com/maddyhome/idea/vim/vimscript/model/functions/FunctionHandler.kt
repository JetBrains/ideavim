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
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.FunctionCallExpression

abstract class FunctionHandler {

  abstract val minimumNumberOfArguments: Int?
  abstract val maximumNumberOfArguments: Int?

  protected abstract fun doFunction(
    argumentValues: List<Expression>,
    editor: Editor,
    context: DataContext,
    vimContext: VimContext,
  ): VimDataType

  fun executeFunction(
    functionCall: FunctionCallExpression,
    editor: Editor,
    context: DataContext,
    vimContext: VimContext,
  ): VimDataType {
    checkFunctionCall(functionCall)
    return doFunction(functionCall.arguments, editor, context, vimContext)
  }

  private fun checkFunctionCall(functionCall: FunctionCallExpression) {
    if (minimumNumberOfArguments != null && functionCall.arguments.size < minimumNumberOfArguments!!) {
      throw ExException("E119: Not enough arguments for function: ${functionCall.functionName}")
    }
    if (maximumNumberOfArguments != null && functionCall.arguments.size > maximumNumberOfArguments!!) {
      throw ExException("E118: Too many arguments for function: ${functionCall.functionName}")
    }
  }
}
