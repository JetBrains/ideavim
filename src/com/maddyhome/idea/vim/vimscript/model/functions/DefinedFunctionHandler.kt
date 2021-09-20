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
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag
import com.maddyhome.idea.vim.vimscript.services.VariableService

data class DefinedFunctionHandler(private val function: FunctionDeclaration) : FunctionHandler() {

  private val logger = logger<DefinedFunctionHandler>()

  override val minimumNumberOfArguments = function.args.size
  override val maximumNumberOfArguments = function.args.size

  override fun doFunction(argumentValues: List<Expression>, editor: Editor, context: DataContext, parent: Executable): VimDataType {
    var result: ExecutionResult = ExecutionResult.Success
    initializeFunctionVariables(argumentValues, editor, context)
    if (function.flags.contains(FunctionFlag.ABORT)) {
      for (statement in function.body) {
        statement.parent = function
        if (result is ExecutionResult.Success) {
          result = statement.execute(editor, context)
        }
      }
      // todo in release 1.9. we should return value AND throw exception
      return when (result) {
        is ExecutionResult.Break -> throw ExException("E587: :break without :while or :for: break")
        is ExecutionResult.Continue -> throw ExException("E586: :continue without :while or :for: continue")
        is ExecutionResult.Success -> VimInt(0)
        is ExecutionResult.Return -> result.value
        is ExecutionResult.Error -> throw ExException("unknown error occurred") // todo
      }
    } else {
      // todo in release 1.9. in this case multiple exceptions can be thrown at once but we don't support it
      // so let's log every exception but show only the last ¯\_(ツ)_/¯
      var lastException: ExException? = null
      for (statement in function.body) {
        statement.parent = function
        try {
          result = statement.execute(editor, context)
          when (result) {
            is ExecutionResult.Break -> throw ExException("E587: :break without :while or :for: break")
            is ExecutionResult.Continue -> throw ExException("E586: :continue without :while or :for: continue")
            is ExecutionResult.Success -> VimInt(0)
            is ExecutionResult.Return -> result.value
            is ExecutionResult.Error -> throw ExException("unknown error occurred") // todo
          }
        } catch (e: ExException) {
          lastException = e
          logger.warn("Caught exception during execution of function with [abort] flag. Exception: ${e.message}")
        }
      }
      if (lastException != null) throw lastException
      return when (result) {
        is ExecutionResult.Success -> VimInt(0)
        is ExecutionResult.Return -> result.value
        else -> throw ExException("unknown error occurred") // todo
      }
    }
  }

  private fun initializeFunctionVariables(argumentValues: List<Expression>, editor: Editor, context: DataContext) {
    for ((index, name) in function.args.withIndex()) {
      VariableService.storeVariable(
        Variable(Scope.FUNCTION_VARIABLE, name),
        argumentValues[index].evaluate(editor, context, function),
        editor,
        context,
        function
      )
    }
  }
}
