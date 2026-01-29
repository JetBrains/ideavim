/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.FinishException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

data class DefinedFunctionHandler(val function: FunctionDeclaration) :
  FunctionHandlerBase<VimDataType>(function.args.size, getMaxArity(function)) {

  private val logger = vimLogger<DefinedFunctionHandler>()
  override val scope: Scope? = function.scope

  init {
    name = function.name
  }

  companion object {
    fun getMaxArity(function: FunctionDeclaration) =
      if (function.hasOptionalArguments) null else function.args.size + function.defaultArgs.size
  }

  override val handlesRange: Boolean
    get() = function.flags.contains(FunctionFlag.RANGE)

  override fun doFunction(
    arguments: Arguments,
    range: LineRange?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val exceptionsCaught = mutableListOf<ExException>()
    val isRangeGiven = (range?.size ?: 0) > 0

    val lineRange = if (range == null || !isRangeGiven) {
      LineRange(editor.currentCaret().getLine(), editor.currentCaret().getLine())
    }
    else {
      range
    }

    initializeFunctionVariables(arguments, lineRange, editor, context, vimContext)

    val returnValue = executeFunctionBody(exceptionsCaught, editor, context)

    if (exceptionsCaught.isNotEmpty()) {
      injector.messages.indicateError()
      injector.messages.showErrorMessage(editor, exceptionsCaught.last().message)
    }
    return returnValue ?: VimInt.ZERO
  }

  private fun executeFunctionBody(
    exceptionsCaught: MutableList<ExException>,
    editor: VimEditor,
    context: ExecutionContext,
  ): VimDataType? {
    var returnValue: VimDataType? = null
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
        is ExecutionResult.Break -> exceptionsCaught.add(exExceptionMessage("E587"))
        is ExecutionResult.Continue -> exceptionsCaught.add(exExceptionMessage("E586"))
        is ExecutionResult.Error -> exceptionsCaught.add(ExException("unknown error occurred")) // todo
        is ExecutionResult.Return -> returnValue = result.value
        is ExecutionResult.Success -> {}
      }
    } else {
      // todo in release 1.9. in this case multiple exceptions can be thrown at once but we don't support it
      for (statement in function.body) {
        statement.vimContext = function
        try {
          result = statement.execute(editor, context)
          when (result) {
            is ExecutionResult.Break -> exceptionsCaught.add(exExceptionMessage("E587"))
            is ExecutionResult.Continue -> exceptionsCaught.add(exExceptionMessage("E586"))
            is ExecutionResult.Error -> exceptionsCaught.add(ExException("unknown error occurred")) // todo
            is ExecutionResult.Return -> {
              returnValue = result.value
              break
            }

            is ExecutionResult.Success -> {}
          }
        } catch (e: ExException) {
          if (e is FinishException) {
            // todo in 1.9: also throw all caught exceptions
            throw FinishException()
          }
          exceptionsCaught.add(e)
          logger.warn("Caught exception during execution of function with [abort] flag. Exception: ${e.message}", e)
        }
      }
    }
    return returnValue
  }

  private fun initializeFunctionVariables(
    arguments: Arguments,
    range: LineRange,
    editor: VimEditor,
    context: ExecutionContext,
    functionCallContext: VimLContext,
  ) {
    // non-optional function arguments
    for ((index, name) in function.args.withIndex()) {
      arguments.setVariable(name, arguments[index], editor, context, function)
    }
    // optional function arguments with default values
    for (index in 0 until function.defaultArgs.size) {
      val expressionToStore = if (index + function.args.size < arguments.size) {
        arguments[index + function.args.size]
      } else {
        function.defaultArgs[index].second.evaluate(editor, context, functionCallContext)
      }
      arguments.setVariable(function.defaultArgs[index].first, expressionToStore, editor, context, function)
    }
    // all the other optional arguments passed to function are stored in a:000 variable
    if (function.hasOptionalArguments) {
      val remainingArgs = if (function.args.size + function.defaultArgs.size < arguments.size) {
        val list = mutableListOf<VimDataType>()
        for (i in function.args.size + function.defaultArgs.size until arguments.size) {
          list.add(arguments[i])
        }
        VimList(list)
      } else {
        VimList(mutableListOf())
      }
      arguments.setVariable("000", remainingArgs, editor, context, function)
    }
    arguments.setVariable("firstline", VimInt(range.startLine1), editor, context, function)
    arguments.setVariable("lastline", VimInt(range.endLine1), editor, context, function)
  }
}
