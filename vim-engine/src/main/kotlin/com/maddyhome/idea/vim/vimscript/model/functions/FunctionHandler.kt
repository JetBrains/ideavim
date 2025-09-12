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

abstract class FunctionHandler {
  lateinit var name: String
  open val scope: Scope? = null
  abstract val minimumNumberOfArguments: Int?
  abstract val maximumNumberOfArguments: Int?
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
    if (minimumNumberOfArguments != null && arguments.size < minimumNumberOfArguments!!) {
      throw exExceptionMessage("E119", name)
    }
    if (maximumNumberOfArguments != null && arguments.size > maximumNumberOfArguments!!) {
      throw exExceptionMessage("E118", name)
    }
  }
}
