package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.EnvVariableExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.OneElementSublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.OptionExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Register
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.AssignmentOperator
import com.maddyhome.idea.vim.vimscript.model.expressions.toOptionScope
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag
import com.maddyhome.idea.vim.vimscript.services.OptionServiceImpl
import com.maddyhome.idea.vim.vimscript.services.VariableService

/**
 * see "h :let"
 */
data class LetCommand(
  val ranges: Ranges,
  val variable: Expression,
  val operator: AssignmentOperator,
  val expression: Expression,
  val isSyntaxSupported: Boolean,
) : Command.SingleExecution(ranges) {

  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  @Throws(ExException::class)
  override fun processCommand(editor: Editor, context: DataContext): ExecutionResult {
    if (!isSyntaxSupported) return ExecutionResult.Error
    when (variable) {
      is Variable -> {
        if (isReadOnlyVariable(variable, editor, context)) {
          throw ExException("E46: Cannot change read-only variable \"${variable.toString(editor, context, parent)}\"")
        }
        val leftValue = VariableService.getNullableVariableValue(variable, editor, context, parent)
        if (leftValue?.isLocked == true && leftValue.lockOwner?.name == variable.name) {
          throw ExException("E741: Value is locked: ${variable.toString(editor, context, parent)}")
        }
        val rightValue = expression.evaluate(editor, context, parent)
        VariableService.storeVariable(variable, operator.getNewValue(leftValue, rightValue), editor, context, this)
      }

      is OneElementSublistExpression -> {
        when (val containerValue = variable.expression.evaluate(editor, context, parent)) {
          is VimDictionary -> {
            val dictKey = VimString(variable.index.evaluate(editor, context, this).asString())
            if (operator != AssignmentOperator.ASSIGNMENT && !containerValue.dictionary.containsKey(dictKey)) {
              throw ExException("E716: Key not present in Dictionary: $dictKey")
            }
            val expressionValue = expression.evaluate(editor, context, this)
            var valueToStore = if (dictKey in containerValue.dictionary) {
              if (containerValue.dictionary[dictKey]!!.isLocked) {
                // todo better exception message
                throw ExException("E741: Value is locked: ${variable.originalString}")
              }
              operator.getNewValue(containerValue.dictionary[dictKey]!!, expressionValue)
            } else {
              if (containerValue.isLocked) {
                // todo better exception message
                throw ExException("E741: Value is locked: ${variable.originalString}")
              }
              expressionValue
            }
            if (valueToStore is VimFuncref && !valueToStore.isSelfFixed &&
              valueToStore.handler is DefinedFunctionHandler &&
              (valueToStore.handler as DefinedFunctionHandler).function.flags.contains(FunctionFlag.DICT)
            ) {
              valueToStore = valueToStore.copy()
              valueToStore.dictionary = containerValue
            }
            containerValue.dictionary[dictKey] = valueToStore
          }
          is VimList -> {
            // we use Integer.parseInt(........asString()) because in case if index's type is Float, List, Dictionary etc
            // vim throws the same error as the asString() method
            val index = Integer.parseInt(variable.index.evaluate(editor, context, this).asString())
            if (index > containerValue.values.size - 1) {
              throw ExException("E684: list index out of range: $index")
            }
            if (containerValue.values[index].isLocked) {
              throw ExException("E741: Value is locked: ${variable.originalString}")
            }
            containerValue.values[index] = operator.getNewValue(containerValue.values[index], expression.evaluate(editor, context, parent))
          }
          is VimBlob -> TODO()
          else -> throw ExException("E689: Can only index a List, Dictionary or Blob")
        }
      }

      is SublistExpression -> {
        if (variable.expression is Variable) {
          val variableValue = VariableService.getNonNullVariableValue(variable.expression, editor, context, this)
          if (variableValue is VimList) {
            // we use Integer.parseInt(........asString()) because in case if index's type is Float, List, Dictionary etc
            // vim throws the same error as the asString() method
            val from = Integer.parseInt(variable.from?.evaluate(editor, context, this)?.toString() ?: "0")
            val to = Integer.parseInt(
              variable.to?.evaluate(editor, context, this)?.toString()
                ?: (variableValue.values.size - 1).toString()
            )

            val expressionValue = expression.evaluate(editor, context, this)
            if (expressionValue !is VimList && expressionValue !is VimBlob) {
              throw ExException("E709: [:] requires a List or Blob value")
            } else if (expressionValue is VimList) {
              if (expressionValue.values.size < to - from + 1) {
                throw ExException("E711: List value does not have enough items")
              } else if (variable.to != null && expressionValue.values.size > to - from + 1) {
                throw ExException("E710: List value has more items than targets")
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
        val optionValue = variable.evaluate(editor, context, parent)
        if (operator == AssignmentOperator.ASSIGNMENT || operator == AssignmentOperator.CONCATENATION ||
          operator == AssignmentOperator.ADDITION || operator == AssignmentOperator.SUBTRACTION
        ) {
          val newValue = operator.getNewValue(optionValue, expression.evaluate(editor, context, this))
          OptionServiceImpl.setOptionValue(variable.scope.toOptionScope(), variable.optionName, newValue, editor, variable.originalString)
        } else {
          TODO()
        }
      }

      is EnvVariableExpression -> TODO()

      is Register -> TODO()

      else -> throw ExException("E121: Undefined variable")
    }
    return ExecutionResult.Success
  }

  private fun isReadOnlyVariable(variable: Variable, editor: Editor, context: DataContext): Boolean {
    if (variable.scope == Scope.FUNCTION_VARIABLE) return true
    if (variable.scope == null && variable.name.evaluate(editor, context, parent).value == "self" && isInsideDictionaryFunction()) return true
    return false
  }

  private fun isInsideDictionaryFunction(): Boolean {
    var node: Executable = this
    while (node !is Script) {
      if (node is FunctionDeclaration && node.flags.contains(FunctionFlag.DICT)) {
        return true
      }
      node = node.parent
    }
    return false
  }
}
