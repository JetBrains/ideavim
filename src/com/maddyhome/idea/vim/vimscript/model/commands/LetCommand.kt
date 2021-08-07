package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.option.ListOption
import com.maddyhome.idea.vim.option.NumberOption
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.StringOption
import com.maddyhome.idea.vim.option.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
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
import com.maddyhome.idea.vim.vimscript.model.expressions.toVimDataType
import com.maddyhome.idea.vim.vimscript.services.VariableService

data class LetCommand(
  val ranges: Ranges,
  val variable: Expression,
  val operator: AssignmentOperator,
  val expression: Expression,
) : Command.SingleExecution(ranges) {

  override val argFlags = flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

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

      // todo local options
      is OptionExpression -> {
        val option = OptionsManager.getOption(variable.optionName) ?: throw ExException("E355: Unknown option: ${variable.optionName}")
        val optionValue = option.toVimDataType()
        if (operator == AssignmentOperator.ASSIGNMENT || operator == AssignmentOperator.CONCATENATION ||
          operator == AssignmentOperator.ADDITION || operator == AssignmentOperator.SUBTRACTION
        ) {
          val newValue = operator.getNewValue(SimpleExpression(optionValue), expression, editor, context, vimContext)
          when (option) {
            is ToggleOption -> {
              if (newValue.asBoolean()) {
                option.set()
              } else {
                option.reset()
              }
            }
            is NumberOption -> {
              if (newValue is VimInt) {
                option.set(newValue.value)
              } else {
                TODO()
              }
            }
            is StringOption -> {
              option.set(newValue.asString())
            }
            is ListOption -> {
              if (newValue is VimList) {
                option.set(newValue.values.joinToString(separator = ",") { it.asString() })
              } else {
                TODO()
              }
            }
            else -> TODO()
          }
        } else {
          // one of the nontrivial vim exceptions should be thrown
          TODO()
        }
      }

      is EnvVariableExpression -> TODO()

      is Register -> TODO()

      else -> throw ExException("E121: Undefined variable")
    }
    return ExecutionResult.Success
  }
}
