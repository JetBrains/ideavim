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
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.functions.BinaryFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler

internal abstract class UnaryFloatFunctionHandlerBase : UnaryFunctionHandler<VimFloat>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimFloat =
    VimFloat(invoke(arguments.getNumberOrFloat(0)))

  protected abstract fun invoke(argument: Double): Double
}

internal abstract class BinaryFloatFunctionHandlerBase : BinaryFunctionHandler<VimFloat>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimFloat =
    VimFloat(invoke(arguments.getNumberOrFloat(0), arguments.getNumberOrFloat(1)))

  protected abstract fun invoke(arg1: Double, arg2: Double): Double
}
