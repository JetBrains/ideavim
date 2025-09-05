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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope

/**
 * Base class for all function handlers
 *
 * Pass in the minimum and maximum number of arguments for the function. If [maxArity] is null, then the function has
 * optional arguments.
 */
abstract class FunctionHandler(protected val minArity: Int = 0, protected val maxArity: Int? = null) {
  constructor(arity: Int) : this(arity, arity)

  lateinit var name: String
  open val scope: Scope? = null
  var range: Range? = null

  protected abstract fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType

  fun executeFunction(
    arguments: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    checkFunctionCall(arguments)
    val result = doFunction(arguments, editor, context, vimContext)
    range = null
    return result
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
}

abstract class UnaryFunctionHandler : FunctionHandler(arity = 1)
abstract class BinaryFunctionHandler : FunctionHandler(arity = 2)
abstract class VariadicFunctionHandler(minArity: Int) : FunctionHandler(minArity, null)
