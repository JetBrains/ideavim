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
import com.maddyhome.idea.vim.vimscript.model.functions.BinaryFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler

internal abstract class UnaryFloatFunctionHandlerBase : UnaryFunctionHandler() {
  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val arg = argumentValues[0].evaluate(editor, context, vimContext)
    val argument = when (arg) {
      is VimFloat -> arg.value
      is VimInt -> arg.value.toDouble()
      else -> throw exExceptionMessage("E808")
    }
    return VimFloat(invoke(argument))
  }

  protected abstract fun invoke(argument: Double): Double
}

internal abstract class BinaryFloatFunctionHandlerBase : BinaryFunctionHandler() {
  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val arg1 = argumentValues[0].evaluate(editor, context, vimContext)
    val arg2 = argumentValues[1].evaluate(editor, context, vimContext)
    val x = when (arg1) {
      is VimFloat -> arg1.value
      is VimInt -> arg1.value.toDouble()
      else -> throw exExceptionMessage("E808")
    }
    val y = when (arg2) {
      is VimFloat -> arg2.value
      is VimInt -> arg2.value.toDouble()
      else -> throw exExceptionMessage("E808")
    }
    return VimFloat(invoke(x, y))
  }

  protected abstract fun invoke(arg1: Double, arg2: Double): Double
}
