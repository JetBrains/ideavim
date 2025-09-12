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
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.register.RegisterConstants
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.expressions.EnvVariableExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.OneElementSublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.OptionExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Register
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.AssignmentOperator
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

/**
 * see "h :let"
 */
@ExCommand(command = "let")
data class LetCommand(
  val range: Range,
  val variable: Expression,
  val operator: AssignmentOperator,
  val expression: Expression,
  val isSyntaxSupported: Boolean,
) : Command.SingleExecution(range, CommandModifier.NONE) {

  private companion object {
    private val logger = vimLogger<LetCommand>()
  }

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  @Throws(ExException::class)
  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    if (!isSyntaxSupported) return ExecutionResult.Error
    when (variable) {
      is Variable -> {
        if ((variable.scope == Scope.SCRIPT_VARIABLE && vimContext.getFirstParentContext() !is Script) ||
          (!isInsideFunction(vimContext) && (variable.scope == Scope.FUNCTION_VARIABLE || variable.scope == Scope.LOCAL_VARIABLE))
        ) {
          throw exExceptionMessage("E461", variable.toString(editor, context, vimContext))
        }

        if (isReadOnlyVariable(variable, editor, context)) {
          throw exExceptionMessage("E46", variable.toString(editor, context, vimContext))
        }

        val leftValue = injector.variableService.getNullableVariableValue(variable, editor, context, vimContext)
        if (leftValue?.isLocked == true && (leftValue.lockOwner as? Variable)?.name == variable.name) {
          throw exExceptionMessage("E741", variable.toString(editor, context, vimContext))
        }
        val rightValue = expression.evaluate(editor, context, vimContext)
        injector.variableService.storeVariable(
          variable,
          operator.getNewValue(leftValue, rightValue),
          editor,
          context,
          this
        )
      }

      is OneElementSublistExpression -> {
        when (val containerValue = variable.expression.evaluate(editor, context, vimContext)) {
          is VimDictionary -> {
            val dictKey = variable.index.evaluate(editor, context, this).toVimString()
            if (operator != AssignmentOperator.ASSIGNMENT && !containerValue.dictionary.containsKey(dictKey)) {
              throw exExceptionMessage("E716", dictKey)
            }
            val expressionValue = expression.evaluate(editor, context, this)
            var valueToStore = if (dictKey in containerValue.dictionary) {
              if (containerValue.dictionary[dictKey]!!.isLocked) {
                throw exExceptionMessage("E741", variable.originalString)
              }
              operator.getNewValue(containerValue.dictionary[dictKey]!!, expressionValue)
            } else {
              if (containerValue.isLocked) {
                throw exExceptionMessage("E741", variable.originalString)
              }
              expressionValue
            }
            if (valueToStore is VimFuncref && !valueToStore.isSelfFixed &&
              valueToStore.handler is DefinedFunctionHandler &&
              valueToStore.handler.function.flags.contains(FunctionFlag.DICT)
            ) {
              valueToStore = valueToStore.copy()
              valueToStore.dictionary = containerValue
            }
            containerValue.dictionary[dictKey] = valueToStore
          }

          is VimList -> {
            val index = variable.index.evaluate(editor, context, this).toVimNumber().value
            if (index > containerValue.values.size - 1) {
              throw exExceptionMessage("E684", index)
            }
            if (containerValue.values[index].isLocked) {
              throw exExceptionMessage("E741", variable.originalString)
            }
            containerValue.values[index] =
              operator.getNewValue(containerValue.values[index], expression.evaluate(editor, context, vimContext))
          }

          is VimBlob -> TODO()
          else -> throw exExceptionMessage("E689")
        }
      }

      is SublistExpression -> {
        if (variable.expression is Variable) {
          val variableValue =
            injector.variableService.getNonNullVariableValue(variable.expression, editor, context, this)
          if (variableValue is VimList) {
            val from = variable.from?.evaluate(editor, context, this)?.toVimNumber()?.value ?: 0
            val to = variable.to?.evaluate(editor, context, this)?.toVimNumber()?.value
              ?: (variableValue.values.size - 1)

            val expressionValue = expression.evaluate(editor, context, this)
            if (expressionValue !is VimList && expressionValue !is VimBlob) {
              throw exExceptionMessage("E709")
            } else if (expressionValue is VimList) {
              if (expressionValue.values.size < to - from + 1) {
                throw exExceptionMessage("E711")
              } else if (variable.to != null && expressionValue.values.size > to - from + 1) {
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

      is OptionExpression -> {
        val optionValue = variable.evaluate(editor, context, vimContext)
        if (operator == AssignmentOperator.ASSIGNMENT || operator == AssignmentOperator.CONCATENATION ||
          operator == AssignmentOperator.ADDITION || operator == AssignmentOperator.SUBTRACTION
        ) {
          val option = injector.optionGroup.getOption(variable.optionName)
            ?: throw exExceptionMessage("E518", variable.originalString)
          val newValue = operator.getNewValue(optionValue, expression.evaluate(editor, context, this))
          when (variable.scope) {
            Scope.GLOBAL_VARIABLE -> injector.optionGroup.setOptionValue(
              option,
              OptionAccessScope.GLOBAL(editor),
              newValue
            )

            Scope.LOCAL_VARIABLE -> injector.optionGroup.setOptionValue(
              option,
              OptionAccessScope.LOCAL(editor),
              newValue
            )

            null -> injector.optionGroup.setOptionValue(option, OptionAccessScope.EFFECTIVE(editor), newValue)
            else -> throw ExException("Invalid option scope")
          }
        } else {
          TODO()
        }
      }

      is EnvVariableExpression -> TODO()

      is Register -> {
        if (RegisterConstants.WRITABLE_REGISTERS.contains(variable.char)) {
          val result = injector.registerGroup.storeText(
            editor,
            context,
            variable.char,
            expression.evaluate(editor, context, vimContext).toVimString().value
          )
          if (!result) {
            logger.error(
              """
              Error during `let ${variable.originalString} ${operator.value} ${expression.originalString}` command execution.
              Could not set register value
              """.trimIndent(),
            )
          }
        } else if (RegisterConstants.VALID_REGISTERS.contains(variable.char)) {
          throw exExceptionMessage("E354", variable.char)
        } else {
          throw exExceptionMessage("E18")
        }
      }

      else -> throw exExceptionMessage("E121", variable.originalString)
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
