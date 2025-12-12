/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.FuncrefCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.NamedFunctionCallExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

/**
 * see "h :call"
 */
@ExCommand(command = "cal[l]")
class CallCommand(val range: Range, val functionCall: Expression) :
  Command.SingleExecution(range, CommandModifier.NONE) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.SELF_SYNCHRONIZED)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    when (functionCall) {
      is NamedFunctionCallExpression -> {
        val scopePrefix = functionCall.scope?.toString() ?: ""
        val name = functionCall.functionName.evaluate(editor, context, vimContext).value
        val function = injector.functionService.getFunctionHandlerOrNull(functionCall.scope, name, vimContext)
        if (function != null) {
          if (function is DefinedFunctionHandler && function.function.flags.contains(FunctionFlag.DICT)) {
            throw exExceptionMessage("E725", scopePrefix + name)
          }
          function.range = range
          function.executeFunction(functionCall.arguments, editor, context, this)
          return ExecutionResult.Success
        }

        val funcref = injector.variableService.getNullableVariableValue(
          VariableExpression(functionCall.scope, functionCall.functionName),
          editor,
          context,
          vimContext
        )
        if (funcref is VimFuncref) {
          funcref.handler.range = range
          funcref.execute(scopePrefix + name, functionCall.arguments, editor, context, vimContext)
          return ExecutionResult.Success
        }

        throw exExceptionMessage("E117", scopePrefix + name)
      }

      is FuncrefCallExpression -> {
        functionCall.evaluateWithRange(range, editor, context, vimContext)
        return ExecutionResult.Success
      }

      else -> {
        // todo add more exceptions
        throw exExceptionMessage("E129")
      }
    }
  }
}
