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
  var range: Range?

  fun executeFunction(
    arguments: List<Expression>,
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
  constructor(arity: Int) : this(arity, arity)

  override lateinit var name: String
  override val scope: Scope? = null
  override var range: Range? = null

  override fun executeFunction(
    arguments: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): T {
    checkFunctionCall(arguments)

    // It's fairly trivial to confirm that Vim eagerly evaluates all function arguments from left to right
    val values = arguments.map { it.evaluate(editor, context, vimContext) }
    val result = doFunction(Arguments(values, editor, context), editor, context, vimContext)
    range = null
    return result
  }

  protected abstract fun doFunction(
    arguments: Arguments,
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

abstract class UnaryFunctionHandler<T : VimDataType> : FunctionHandlerBase<T>(arity = 1)
abstract class BinaryFunctionHandler<T : VimDataType> : FunctionHandlerBase<T>(arity = 2)
abstract class VariadicFunctionHandler<T : VimDataType>(minArity: Int) : FunctionHandlerBase<T>(minArity, null)
