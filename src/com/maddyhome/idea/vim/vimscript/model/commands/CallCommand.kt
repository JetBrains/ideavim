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

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.FuncrefCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.FunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag
import com.maddyhome.idea.vim.vimscript.services.FunctionStorage
import com.maddyhome.idea.vim.vimscript.services.VariableService

/**
 * see "h :call"
 */
class CallCommand(val ranges: Ranges, val functionCall: Expression) : Command.SingleExecution(ranges) {

  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun processCommand(editor: Editor, context: DataContext): ExecutionResult {
    if (functionCall is FunctionCallExpression) {
      val function = FunctionStorage.getFunctionHandlerOrNull(functionCall.scope, functionCall.functionName, parent)
      if (function != null) {
        if (function is DefinedFunctionHandler && function.function.flags.contains(FunctionFlag.DICT)) {
          throw ExException(
            "E725: Calling dict function without Dictionary: " +
              ((if (functionCall.scope != null) functionCall.scope.c + ":" else "") + functionCall.functionName)
          )
        }
        function.ranges = ranges
        function.executeFunction(functionCall.arguments, editor, context, this)
        return ExecutionResult.Success
      }

      val funcref = VariableService.getNullableVariableValue(Variable(functionCall.scope, functionCall.functionName), editor, context, parent)
      if (funcref is VimFuncref) {
        if (funcref.handler is DefinedFunctionHandler && funcref.handler.function.flags.contains(FunctionFlag.DICT) && funcref.handler.function.self == null) {
          throw ExException(
            "E725: Calling dict function without Dictionary: " +
              ((if (functionCall.scope != null) functionCall.scope.c + ":" else "") + functionCall.functionName)
          )
        }
        funcref.handler.ranges = ranges
        funcref.execute(functionCall.arguments, editor, context, parent)
        return ExecutionResult.Success
      }

      throw ExException("E117: Unknown function: ${if (functionCall.scope != null) functionCall.scope.c + ":" else ""}${functionCall.functionName}")
    } else if (functionCall is FuncrefCallExpression) {
      functionCall.evaluateWithRange(ranges, editor, context, parent)
      return ExecutionResult.Success
    } else {
      // todo add more exceptions
      throw ExException("E129: Function name required")
    }
  }
}
