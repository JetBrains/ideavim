/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.bitwiseFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

internal abstract class BinaryBitwiseFunctionHandlerBase : FunctionHandler() {
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

    if (arg1 is VimFloat || arg2 is VimFloat) {
      throw exExceptionMessage("E805")  // E805: Using a Float as a Number
    }

    return VimInt(invoke(arg1.asDouble().toInt(), arg2.asDouble().toInt()))
  }

  abstract fun invoke(a: Int, b: Int): Int
}

@VimscriptFunction(name = "and")
internal class AndFunctionHandler : BinaryBitwiseFunctionHandlerBase() {
  override fun invoke(a: Int, b: Int) = a and b
}

@VimscriptFunction(name = "or")
internal class OrFunctionHandler : BinaryBitwiseFunctionHandlerBase() {
  override fun invoke(a: Int, b: Int) = a or b
}

@VimscriptFunction(name = "xor")
internal class XorFunctionHandler : BinaryBitwiseFunctionHandlerBase() {
  override fun invoke(a: Int, b: Int) = a xor b
}
