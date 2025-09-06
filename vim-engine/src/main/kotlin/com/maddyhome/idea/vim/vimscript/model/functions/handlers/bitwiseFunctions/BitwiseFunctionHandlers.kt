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
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.functions.BinaryFunctionHandler

internal abstract class BinaryBitwiseFunctionHandlerBase : BinaryFunctionHandler<VimInt>() {
  override fun doFunction(arguments: Arguments, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext) =
    VimInt(invoke(arguments.getNumber(0).value, arguments.getNumber(1).value))

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
