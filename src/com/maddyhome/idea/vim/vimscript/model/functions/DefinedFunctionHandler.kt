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
import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.LineNumberRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
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
    var returnValue: VimDataType? = null
    val exceptionsCaught = mutableListOf<ExException>()
    val isRangeGiven = (ranges?.rangesCount ?: 0) > 0

    if (!isRangeGiven) {
      val currentLine = editor.caretModel.currentCaret.logicalPosition.line
      ranges = Ranges()
      ranges!!.addRange(
        arrayOf(
          LineNumberRange(currentLine, 0, false),
          LineNumberRange(currentLine, 0, false)
        )
      )
    }
    initializeFunctionVariables(argumentValues, editor, context)

    if (function.flags.contains(FunctionFlag.RANGE)) {
      val line = (VariableService.getNonNullVariableValue(Variable(Scope.FUNCTION_VARIABLE, "firstline"), editor, context, function) as VimInt).value
      returnValue = executeBodyForLine(line, isRangeGiven, exceptionsCaught, editor, context)
    } else {
      val firstLine = (VariableService.getNonNullVariableValue(Variable(Scope.FUNCTION_VARIABLE, "firstline"), editor, context, function) as VimInt).value
      val lastLine = (VariableService.getNonNullVariableValue(Variable(Scope.FUNCTION_VARIABLE, "lastline"), editor, context, function) as VimInt).value
      for (line in firstLine..lastLine) {
        returnValue = executeBodyForLine(line, isRangeGiven, exceptionsCaught, editor, context)
      }
    }

    if (exceptionsCaught.isNotEmpty()) {
      VimPlugin.indicateError()
      VimPlugin.showMessage(exceptionsCaught.last().message)
    }
    return returnValue ?: VimInt(0)
  }

  private fun executeBodyForLine(line: Int, isRangeGiven: Boolean, exceptionsCaught: MutableList<ExException>, editor: Editor, context: DataContext): VimDataType? {
    var returnValue: VimDataType? = null
    if (isRangeGiven) {
      editor.caretModel.moveToLogicalPosition(LogicalPosition(line - 1, 0))
    }
    var result: ExecutionResult = ExecutionResult.Success
    if (function.flags.contains(FunctionFlag.ABORT)) {
      for (statement in function.body) {
        statement.parent = function
        if (result is ExecutionResult.Success) {
          result = statement.execute(editor, context)
        }
      }
      // todo in release 1.9. we should return value AND throw exception
      when (result) {
        is ExecutionResult.Break -> exceptionsCaught.add(ExException("E587: :break without :while or :for: break"))
        is ExecutionResult.Continue -> exceptionsCaught.add(ExException("E586: :continue without :while or :for: continue"))
        is ExecutionResult.Error -> exceptionsCaught.add(ExException("unknown error occurred")) // todo
        is ExecutionResult.Return -> returnValue = result.value
        is ExecutionResult.Success -> {}
      }
    } else {
      // todo in release 1.9. in this case multiple exceptions can be thrown at once but we don't support it
      for (statement in function.body) {
        statement.parent = function
        try {
          result = statement.execute(editor, context)
          when (result) {
            is ExecutionResult.Break -> exceptionsCaught.add(ExException("E587: :break without :while or :for: break"))
            is ExecutionResult.Continue -> exceptionsCaught.add(ExException("E586: :continue without :while or :for: continue"))
            is ExecutionResult.Error -> exceptionsCaught.add(ExException("unknown error occurred")) // todo
            is ExecutionResult.Return -> {
              returnValue = result.value
              break
            }
            is ExecutionResult.Success -> {}
          }
        } catch (e: ExException) {
          exceptionsCaught.add(e)
          logger.warn("Caught exception during execution of function with [abort] flag. Exception: ${e.message}")
        }
      }
    }
    return returnValue
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
    VariableService.storeVariable(
      Variable(Scope.FUNCTION_VARIABLE, "firstline"),
      VimInt(ranges!!.getFirstLine(editor, editor.caretModel.currentCaret) + 1), editor, context, function
    )
    VariableService.storeVariable(
      Variable(Scope.FUNCTION_VARIABLE, "lastline"),
      VimInt(ranges!!.getLine(editor, editor.caretModel.currentCaret) + 1), editor, context, function
    )
  }
}
