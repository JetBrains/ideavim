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
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.services.VariableService

data class DefinedFunctionHandler(
  private val argumentNames: List<String>,
  private val body: List<Executable>,
) : FunctionHandler() {

  override val minimumNumberOfArguments = argumentNames.size
  override val maximumNumberOfArguments = argumentNames.size

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: Editor,
    context: DataContext,
    vimContext: VimContext,
  ): VimDataType {
    var result: ExecutionResult = ExecutionResult.Success
    vimContext.enterFunction()
    try {
      initializeFunctionVariables(argumentValues, editor, context, vimContext)
      for (statement in body) {
        if (result is ExecutionResult.Success) {
          result = statement.execute(editor, context, vimContext)
        }
      }
    } catch (e: ExException) {
      vimContext.leaveFunction()
      throw e
    }
    vimContext.leaveFunction()
    return when (result) {
      is ExecutionResult.Break -> throw ExException("E587: :break without :while or :for: break")
      is ExecutionResult.Continue -> throw ExException("E586: :continue without :while or :for: continue")
      is ExecutionResult.Success -> VimInt(0)
      is ExecutionResult.Return -> result.value
      is ExecutionResult.Error -> throw ExException("unknown error occurred") // todo
    }
  }

  private fun initializeFunctionVariables(
    argumentValues: List<Expression>,
    editor: Editor,
    context: DataContext,
    vimContext: VimContext,
  ) {
    for ((index, name) in argumentNames.withIndex()) {
      VariableService.storeVariable(
        Variable(Scope.FUNCTION_VARIABLE, name),
        argumentValues[index].evaluate(editor, context, vimContext),
        editor,
        context,
        vimContext
      )
    }
  }
}
