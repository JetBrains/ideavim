package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.EnvVariableExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.OneElementSublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.OptionExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Register
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.AssignmentOperator
import com.maddyhome.idea.vim.vimscript.services.VariableService

data class LetCommand(
  val ranges: Ranges,
  val variable: Expression,
  val operator: AssignmentOperator,
  val expression: Expression,
) : Command.SingleExecution(ranges) {

  override val argFlags = CommandHandlerFlags(RangeFlag.RANGE_FORBIDDEN, Access.READ_ONLY, emptySet())

  @Throws(ExException::class)
  override fun processCommand(editor: Editor, context: DataContext, vimContext: VimContext): ExecutionResult {
    when (variable) {
      is Variable -> {
        VariableService.storeVariable(
          variable, operator.getNewValue(variable, expression, editor, context, vimContext),
          editor, context, vimContext
        )
      }

      is OneElementSublistExpression -> {
        if (variable.expression is Variable) {
          val variableValue = VariableService.getNonNullVariableValue(variable.expression, editor, context, vimContext)
          when (variableValue) {
            is VimDictionary -> {
              val dictKey = VimString(variable.index.evaluate(editor, context, vimContext).asString())
              if (operator != AssignmentOperator.ASSIGNMENT && !variableValue.dictionary.containsKey(dictKey)) {
                throw ExException("E716: Key not present in Dictionary: $dictKey")
              }
              if (variableValue.dictionary.containsKey(dictKey)) {
                variableValue.dictionary[dictKey] =
                  operator.getNewValue(
                    SimpleExpression(variableValue.dictionary[dictKey]!!), expression, editor,
                    context, vimContext
                  )
              } else {
                variableValue.dictionary[dictKey] = expression.evaluate(editor, context, vimContext)
              }
            }
            is VimList -> {
              // we use Integer.parseInt(........asString()) because in case if index's type is Float, List, Dictionary etc
              // vim throws the same error as the asString() method
              val index = Integer.parseInt(variable.index.evaluate(editor, context, vimContext).asString())
              if (index > variableValue.values.size - 1) {
                throw ExException("E684: list index out of range: $index")
              }
              variableValue.values[index] = operator.getNewValue(
                SimpleExpression(variableValue.values[index]), expression, editor, context, vimContext
              )
            }
            is VimBlob -> TODO()
            else -> throw ExException("E689: Can only index a List, Dictionary or Blob")
          }
        } else {
          throw ExException("E121: Undefined variable")
        }
      }

      is SublistExpression -> {
        if (variable.expression is Variable) {
          val variableValue = VariableService.getNonNullVariableValue(variable.expression, editor, context, vimContext)
          if (variableValue is VimList) {
            // we use Integer.parseInt(........asString()) because in case if index's type is Float, List, Dictionary etc
            // vim throws the same error as the asString() method
            val from = Integer.parseInt(variable.from?.evaluate(editor, context, vimContext)?.toString() ?: "0")
            val to = Integer.parseInt(
              variable.to?.evaluate(editor, context, vimContext)?.toString()
                ?: (variableValue.values.size - 1).toString()
            )

            val expressionValue = expression.evaluate(editor, context, vimContext)
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

      is OptionExpression -> TODO() // they can be local and global btw

      is EnvVariableExpression -> TODO()

      is Register -> TODO()

      else -> throw ExException("E121: Undefined variable")
    }
    return ExecutionResult.Success
  }
}
