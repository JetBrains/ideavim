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
import com.maddyhome.idea.vim.ex.exExceptionMessage
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

    // It's fairly trivial to confirm that Vim eagerly evaluates all function arguments from left to right
    val values = arguments.map { it.evaluate(editor, context, vimContext) }
    return doFunction(Arguments(values, editor, context), range, editor, context, vimContext)
  }

  protected abstract fun doFunction(
    arguments: Arguments,
    range: Range?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): T

  private fun checkFunctionCall(arguments: List<Expression>) {
    if (arguments.size < minArity) {
      throw exExceptionMessage("E119", name)
    }

    // If the function has optional arguments, then maxArity will be null
    if (maxArity != null && arguments.size > maxArity) {
      throw exExceptionMessage("E118", name)
    }
  }

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

  override fun doFunction(
    arguments: Arguments,
    range: Range?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext
  ): T {
    // TODO: Builtin functions don't handle range. Are there any that should?
    // Vim moves the caret to the start of the first line in the range before the function is invoked. If the function
    // handles range, it is only called once. Otherwise, Vim moves the caret to the start of each line in the range and
    // calls the function multiple times. It does this with builtin functions too, as demonstrated by:
    // `'<,'>call add(l, 12)`
    // This will fill the List `l` with items for each line in the range, and leave the caret at the end of the range.
    // IdeaVim does not currently support this.
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
