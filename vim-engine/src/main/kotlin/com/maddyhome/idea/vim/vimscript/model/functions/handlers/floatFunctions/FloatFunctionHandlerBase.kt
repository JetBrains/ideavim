/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.floatFunctions

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

internal abstract class UnaryFloatFunctionHandlerBase : FunctionHandler() {
  override val minimumNumberOfArguments = 1
  override val maximumNumberOfArguments = 1

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val argument = argumentValues[0].evaluate(editor, context, vimContext)
    if (argument !is VimFloat && argument !is VimInt) {
      throw exExceptionMessage("E808")  // E808: Number or Float required
    }
    return VimFloat(invoke(argument.asDouble()))
  }

  protected abstract fun invoke(argument: Double): Double
}

internal abstract class BinaryFloatFunctionHandlerBase : FunctionHandler() {
  override val minimumNumberOfArguments = 2
  override val maximumNumberOfArguments = 2

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val arg1 = argumentValues[0].evaluate(editor, context, vimContext)
    val arg2 = argumentValues[1].evaluate(editor, context, vimContext)
    if ((arg1 !is VimFloat && arg1 !is VimInt) || (arg2 !is VimFloat && arg2 !is VimInt)) {
      throw exExceptionMessage("E808")  // E808: Number or Float required
    }
    return VimFloat(invoke(arg1.asDouble(), arg2.asDouble()))
  }

  protected abstract fun invoke(arg1: Double, arg2: Double): Double
}
