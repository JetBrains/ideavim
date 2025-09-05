/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.floatFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler

@VimscriptFunction(name = "isinf")
internal class IsInfFunctionHandler : UnaryFunctionHandler() {
  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val argument = argumentValues[0].evaluate(editor, context, vimContext)
    if (argument !is VimFloat) {
      return false.asVimInt()
    }
    return when (argument.toVimFloat().value) {
      Double.POSITIVE_INFINITY -> VimInt.ONE
      Double.NEGATIVE_INFINITY -> VimInt.MINUS_ONE
      else -> VimInt.ZERO
    }
  }
}

@VimscriptFunction("isnan")
internal class IsNanFunctionHandler : UnaryFunctionHandler() {
  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val argument = argumentValues[0].evaluate(editor, context, vimContext)
    if (argument !is VimFloat) {
      return false.asVimInt()
    }
    return argument.toVimFloat().value.isNaN().asVimInt()
  }
}
