/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.FinishException
import com.maddyhome.idea.vim.ex.ranges.LineNumberRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

public data class DefinedFunctionHandler(val function: FunctionDeclaration) : FunctionHandler() {
  private val logger = vimLogger<DefinedFunctionHandler>()
  override val scope: Scope? = function.scope
  override val minimumNumberOfArguments: Int = function.args.size
  override val maximumNumberOfArguments: Int? get() = if (function.hasOptionalArguments) null else function.args.size + function.defaultArgs.size

  init {
    name = function.name
  }

  override fun doFunction(argumentValues: List<Expression>, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    var returnValue: VimDataType? = null
    val exceptionsCaught = mutableListOf<ExException>()
    val isRangeGiven = (ranges?.size() ?: 0) > 0

    if (!isRangeGiven) {
      val currentLine = editor.currentCaret().getBufferPosition().line
      ranges = Ranges()
      ranges!!.addRange(
        arrayOf(
          LineNumberRange(currentLine, 0, false),
          LineNumberRange(currentLine, 0, false),
        ),
      )
    }
    initializeFunctionVariables(argumentValues, editor, context, vimContext)

    if (function.flags.contains(FunctionFlag.RANGE)) {
      val line = (injector.variableService.getNonNullVariableValue(Variable(Scope.FUNCTION_VARIABLE, "firstline"), editor, context, function) as VimInt).value
      returnValue = executeBodyForLine(line, isRangeGiven, exceptionsCaught, editor, context)
    } else {
      val firstLine = (injector.variableService.getNonNullVariableValue(Variable(Scope.FUNCTION_VARIABLE, "firstline"), editor, context, function) as VimInt).value
      val lastLine = (injector.variableService.getNonNullVariableValue(Variable(Scope.FUNCTION_VARIABLE, "lastline"), editor, context, function) as VimInt).value
      for (line in firstLine..lastLine) {
        returnValue = executeBodyForLine(line, isRangeGiven, exceptionsCaught, editor, context)
      }
    }

    if (exceptionsCaught.isNotEmpty()) {
      injector.messages.indicateError()
      injector.messages.showStatusBarMessage(editor, exceptionsCaught.last().message)
    }
    return returnValue ?: VimInt(0)
  }

  private fun executeBodyForLine(line: Int, isRangeGiven: Boolean, exceptionsCaught: MutableList<ExException>, editor: VimEditor, context: ExecutionContext): VimDataType? {
    var returnValue: VimDataType? = null
    if (isRangeGiven) {
      editor.currentCaret().moveToBufferPosition(BufferPosition(line - 1, 0))
    }
    var result: ExecutionResult = ExecutionResult.Success
    if (function.flags.contains(FunctionFlag.ABORT)) {
      for (statement in function.body) {
        statement.vimContext = function
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
        is ExecutionResult.Success -> {
        }
      }
    } else {
      // todo in release 1.9. in this case multiple exceptions can be thrown at once but we don't support it
      for (statement in function.body) {
        statement.vimContext = function
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
            is ExecutionResult.Success -> {
            }
          }
        } catch (e: ExException) {
          if (e is FinishException) {
            // todo in 1.9: also throw all caught exceptions
            throw FinishException()
          }
          exceptionsCaught.add(e)
          logger.warn("Caught exception during execution of function with [abort] flag. Exception: ${e.message}")
        }
      }
    }
    return returnValue
  }

  private fun initializeFunctionVariables(argumentValues: List<Expression>, editor: VimEditor, context: ExecutionContext, functionCallContext: VimLContext) {
    // non-optional function arguments
    for ((index, name) in function.args.withIndex()) {
      injector.variableService.storeVariable(
        Variable(Scope.FUNCTION_VARIABLE, name),
        argumentValues[index].evaluate(editor, context, functionCallContext),
        editor,
        context,
        function,
      )
    }
    // optional function arguments with default values
    for (index in 0 until function.defaultArgs.size) {
      val expressionToStore = if (index + function.args.size < argumentValues.size) argumentValues[index + function.args.size] else function.defaultArgs[index].second
      injector.variableService.storeVariable(
        Variable(Scope.FUNCTION_VARIABLE, function.defaultArgs[index].first),
        expressionToStore.evaluate(editor, context, functionCallContext),
        editor,
        context,
        function,
      )
    }
    // all the other optional arguments passed to function are stored in a:000 variable
    if (function.hasOptionalArguments) {
      val remainingArgs = if (function.args.size + function.defaultArgs.size < argumentValues.size) {
        VimList(
          argumentValues.subList(function.args.size + function.defaultArgs.size, argumentValues.size)
            .map { it.evaluate(editor, context, functionCallContext) }.toMutableList(),
        )
      } else {
        VimList(mutableListOf())
      }
      injector.variableService.storeVariable(
        Variable(Scope.FUNCTION_VARIABLE, "000"),
        remainingArgs,
        editor,
        context,
        function,
      )
    }
    injector.variableService.storeVariable(
      Variable(Scope.FUNCTION_VARIABLE, "firstline"),
      VimInt(ranges!!.getFirstLine(editor, editor.currentCaret()) + 1),
      editor,
      context,
      function,
    )
    injector.variableService.storeVariable(
      Variable(Scope.FUNCTION_VARIABLE, "lastline"),
      VimInt(ranges!!.getLine(editor, editor.currentCaret()) + 1),
      editor,
      context,
      function,
    )
  }
}
