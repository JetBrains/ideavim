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

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
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

/**
 * see "h :call"
 */
class CallCommand(val ranges: Ranges, val functionCall: Expression) : Command.SingleExecution(ranges) {

  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun processCommand(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments): ExecutionResult {
    if (functionCall is FunctionCallExpression) {
      val function = injector.functionService.getFunctionHandlerOrNull(
        functionCall.scope,
        functionCall.functionName.evaluate(editor, context, vimContext).value,
        vimContext
      )
      if (function != null) {
        if (function is DefinedFunctionHandler && function.function.flags.contains(FunctionFlag.DICT)) {
          throw ExException(
            "E725: Calling dict function without Dictionary: " +
              (functionCall.scope?.toString() ?: "") + functionCall.functionName.evaluate(editor, context, vimContext)
          )
        }
        function.ranges = ranges
        function.executeFunction(functionCall.arguments, editor, context, this)
        return ExecutionResult.Success
      }

      val name = (functionCall.scope?.toString() ?: "") + functionCall.functionName.evaluate(editor, context, vimContext)
      val funcref = injector.variableService.getNullableVariableValue(Variable(functionCall.scope, functionCall.functionName), editor, context, vimContext)
      if (funcref is VimFuncref) {
        funcref.handler.ranges = ranges
        funcref.execute(name, functionCall.arguments, editor, context, vimContext)
        return ExecutionResult.Success
      }

      throw ExException("E117: Unknown function: $name")
    } else if (functionCall is FuncrefCallExpression) {
      functionCall.evaluateWithRange(ranges, editor, context, vimContext)
      return ExecutionResult.Success
    } else {
      // todo add more exceptions
      throw ExException("E129: Function name required")
    }
  }
}
