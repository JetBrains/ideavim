/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import com.maddyhome.idea.vim.vimscript.services.FunctionStorage

internal class SubmatchFunctionHandler : FunctionHandler() {
  override val name = "submatch"
  override val minimumNumberOfArguments = 1
  override val maximumNumberOfArguments = 2

  var latestMatch: String = ""

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val firstArgValue = argumentValues[0].evaluate(editor, context, vimContext).toVimNumber()
    if (firstArgValue.value != 0 || argumentValues.size > 1) {
      throw ExException("Sorry, only `submatch(0)` is supported :(")
    }
    return VimString(latestMatch)
  }

  companion object {
    fun getInstance(): SubmatchFunctionHandler {
      return FunctionStorage.getFunctionOfType<SubmatchFunctionHandler>()
    }
  }
}
