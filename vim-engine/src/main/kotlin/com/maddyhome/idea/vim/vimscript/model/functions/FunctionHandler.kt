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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression

interface FunctionHandler {
  val name: String
  val scope: Scope?

  /**
   * Execute the function with the given arguments
   *
   * The function can be called with a range. If not provided, the current line will be used. If the range is given, the
   * caret is moved to the start of the first line in the range before the function is invoked. If the function handles
   * range (see `:help :func-range`), then it is only called once. Otherwise it is called for each line in the range,
   * with the caret moved to the start of the next line before each call.
   */
  fun executeFunction(
    arguments: List<Expression>,
    range: Range?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType
}

/**
 * Base class for all function handlers
 *
 * Pass in the minimum and maximum number of arguments for the function. If [maxArity] is null, then the function has
 * optional arguments.
 */
abstract class FunctionHandlerBase<T : VimDataType>(protected val minArity: Int = 0, protected val maxArity: Int? = null)
  : FunctionHandler {
  override lateinit var name: String
  override val scope: Scope? = null

  override fun executeFunction(
    arguments: List<Expression>,
    range: Range?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): T {
    checkFunctionCall(arguments)

    // If we're given a range, it is up to the function how we handle it. If the function handles it, move to the start
    // of the range and let it get on with things. If the function does not handle it, we move to the start of each line
    // in the range and then call the function repeatedly.
    // The arguments are re-evaluated for each line. This is easily demonstrated with e.g. `add({list}, getline('.'))`.
    // Note that builtin functions do not handle range, so are called multiple times. User-defined functions have to
    // opt in to range handling. See `:help :func-range`.
    val hasRange = range != null && range.size() > 0
    if (hasRange) {
      val lineRange = range.getLineRange(editor, editor.currentCaret())
      if (lineRange.startLine1 > editor.lineCount() || lineRange.endLine1 > editor.lineCount()) {
        throw exExceptionMessage("E16")
      }

      // Regardless of how handles the range, we always pass it to the handler. User-defined functions always have
      // `a:firstline` and `a:lastline` set.
      if (handlesRange) {
        editor.currentCaret().moveToBufferPosition(BufferPosition(lineRange.startLine, 0))
        return doFunction(arguments, lineRange, editor, context, vimContext)
      }
      else {
        var lastRetval: T? = null
        for (line in lineRange.startLine..lineRange.endLine) {
          editor.currentCaret().moveToBufferPosition(BufferPosition(line, 0))
          lastRetval = doFunction(arguments, lineRange, editor, context, vimContext)
        }
        return lastRetval ?: throw exExceptionMessage("E16")
      }
    }
    else {
      // No range. Don't move the caret and just call the function.
      return doFunction(arguments, null, editor, context, vimContext)
    }
  }

  private fun checkFunctionCall(arguments: List<Expression>) {
    if (arguments.size < minArity) {
      throw exExceptionMessage("E119", name)
    }

    // If the function has optional arguments, then maxArity will be null
    if (maxArity != null && arguments.size > maxArity) {
      throw exExceptionMessage("E118", name)
    }
  }

  protected abstract val handlesRange: Boolean

  private fun doFunction(
    arguments: List<Expression>,
    lineRange: LineRange?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): T {
    val values = arguments.map { it.evaluate(editor, context, vimContext) }
    return doFunction(Arguments(values, editor, context), lineRange, editor, context, vimContext)
  }

  protected abstract fun doFunction(
    arguments: Arguments,
    range: LineRange?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): T

  protected class Arguments(
    private val arguments: List<VimDataType>,
    private val editor: VimEditor,
    private val context: ExecutionContext,
  ) {
    val size = arguments.size

    operator fun get(index: Int) = arguments[index]
    fun getOrNull(index: Int) = arguments.getOrNull(index)

    fun getNumber(index: Int) = arguments[index].toVimNumber()
    fun getNumberOrNull(index: Int) = arguments.getOrNull(index)?.toVimNumber()
    fun getString(index: Int) = arguments[index].toVimString()
    fun getStringOrNull(index: Int) = arguments.getOrNull(index)?.toVimString()

    fun getNumberOrFloat(index: Int) = when(arguments[index]) {
      is VimFloat -> arguments[index].toVimFloat().value
      is VimInt -> arguments[index].toVimNumber().value.toDouble()
      else -> throw exExceptionMessage("E808")  // E808: Number or Float required
    }

    fun getVariable(name: String, vimContext: VimLContext): VimDataType {
      return injector.variableService.getNonNullVariableValue(
        VariableExpression(Scope.FUNCTION_VARIABLE, name),
        editor,
        context,
        vimContext
      )
    }

    fun setVariable(
      name: String,
      value: VimDataType,
      editor: VimEditor,
      context: ExecutionContext,
      vimContext: VimLContext,
    ) {
      injector.variableService.storeVariable(
        VariableExpression(Scope.FUNCTION_VARIABLE, name),
        value,
        editor,
        context,
        vimContext,
      )
    }
  }
}

abstract class BuiltinFunctionHandler<T : VimDataType>(minArity: Int = 0, maxArity: Int? = null)
  : FunctionHandlerBase<T>(minArity, maxArity) {
  constructor(arity: Int) : this(arity, arity)

  // Builtin functions do not handle range. They are called multiple times, once for each line in the range, after the
  // caret is moved.
  override val handlesRange: Boolean = false

  override fun doFunction(
    arguments: Arguments,
    range: LineRange?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext
  ): T {
    return doFunction(arguments, editor, context, vimContext)
  }

  protected abstract fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext
  ): T
}

abstract class UnaryFunctionHandler<T : VimDataType> : BuiltinFunctionHandler<T>(arity = 1)
abstract class BinaryFunctionHandler<T : VimDataType> : BuiltinFunctionHandler<T>(arity = 2)
abstract class VariadicFunctionHandler<T : VimDataType>(minArity: Int) : BuiltinFunctionHandler<T>(minArity, null)
