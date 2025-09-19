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
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.expressions.EnvVariableExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.LValueExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.AssignmentOperator
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

/**
 * see "h :let"
 */
@ExCommand(command = "let")
data class LetCommand(
  val range: Range,
  val lvalue: Expression,
  val operator: AssignmentOperator,
  val expression: Expression,
  val isSyntaxSupported: Boolean,
  val assignmentTextForErrors: String
) : Command.SingleExecution(range, CommandModifier.NONE) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  @Throws(ExException::class)
  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    if (!isSyntaxSupported) return ExecutionResult.Error

    if (lvalue is LValueExpression) {
      val currentValue =
        if (operator != AssignmentOperator.ASSIGNMENT) lvalue.evaluate(editor, context, vimContext) else null
      val rhs = expression.evaluate(editor, context, vimContext)
      val newValue = operator.getNewValue(currentValue, rhs, lvalue.isStronglyTyped())
      lvalue.assign(newValue, editor, context, this, assignmentTextForErrors)
      return ExecutionResult.Success
    }

    when (lvalue) {
      is Variable -> {
        if ((lvalue.scope == Scope.SCRIPT_VARIABLE && vimContext.getFirstParentContext() !is Script) ||
          (!isInsideFunction(vimContext) && (lvalue.scope == Scope.FUNCTION_VARIABLE || lvalue.scope == Scope.LOCAL_VARIABLE))
        ) {
          throw exExceptionMessage("E461", lvalue.toString(editor, context, vimContext))
        }

        if (isReadOnlyVariable(lvalue, editor, context)) {
          throw exExceptionMessage("E46", lvalue.toString(editor, context, vimContext))
        }

        val leftValue = injector.variableService.getNullableVariableValue(lvalue, editor, context, vimContext)
        if (leftValue?.isLocked == true && (leftValue.lockOwner as? Variable)?.name == lvalue.name) {
          throw exExceptionMessage("E741", lvalue.toString(editor, context, vimContext))
        }
        val rightValue = expression.evaluate(editor, context, vimContext)
        injector.variableService.storeVariable(
          lvalue,
          operator.getNewValue(leftValue, rightValue, false),
          editor,
          context,
          this
        )
      }

      is SublistExpression -> {
        if (lvalue.expression is Variable) {
          val variableValue =
            injector.variableService.getNonNullVariableValue(lvalue.expression, editor, context, this)
          if (variableValue is VimList) {
            val from = lvalue.from?.evaluate(editor, context, this)?.toVimNumber()?.value ?: 0
            val to = lvalue.to?.evaluate(editor, context, this)?.toVimNumber()?.value
              ?: (variableValue.values.size - 1)

            val expressionValue = expression.evaluate(editor, context, this)
            if (expressionValue !is VimList && expressionValue !is VimBlob) {
              throw exExceptionMessage("E709")
            } else if (expressionValue is VimList) {
              if (expressionValue.values.size < to - from + 1) {
                throw exExceptionMessage("E711")
              } else if (lvalue.to != null && expressionValue.values.size > to - from + 1) {
                throw exExceptionMessage("E710")
              }
              val newListSize = expressionValue.values.size - (to - from + 1) + variableValue.values.size
              var i = from
              if (newListSize > variableValue.values.size) {
                while (i < variableValue.values.size) {
                  variableValue.values[i] = expressionValue.values[i - from]
                  i += 1
                }
                while (i < newListSize) {
                  variableValue.values.add(expressionValue.values[i - from])
                  i += 1
                }
              } else {
                while (i <= to) {
                  variableValue.values[i] = expressionValue.values[i - from]
                  i += 1
                }
              }
            } else if (expressionValue is VimBlob) {
              TODO()
            }
          } else {
            throw ExException("wrong variable type")
          }
        }
      }

      is EnvVariableExpression -> TODO()

      else -> throw exExceptionMessage("E121", lvalue.originalString)
    }
    return ExecutionResult.Success
  }

  private fun isInsideFunction(vimLContext: VimLContext): Boolean {
    var isInsideFunction = false
    var node = vimLContext
    while (!node.isFirstParentContext()) {
      if (node is FunctionDeclaration) {
        isInsideFunction = true
      }
      node = node.getPreviousParentContext()
    }
    return isInsideFunction
  }

  private fun isReadOnlyVariable(variable: Variable, editor: VimEditor, context: ExecutionContext): Boolean {
    if (variable.scope == Scope.FUNCTION_VARIABLE) return true
    if (variable.scope == null && variable.name.evaluate(
        editor,
        context,
        vimContext
      ).value == "self" && isInsideDictionaryFunction()
    ) return true
    return false
  }

  private fun isInsideDictionaryFunction(): Boolean {
    var node: VimLContext = this
    while (!node.isFirstParentContext()) {
      if (node is FunctionDeclaration && node.flags.contains(FunctionFlag.DICT)) {
        return true
      }
      node = node.getPreviousParentContext()
    }
    return false
  }
}
