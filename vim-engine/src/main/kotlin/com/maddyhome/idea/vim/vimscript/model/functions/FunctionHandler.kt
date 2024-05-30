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
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope

public abstract class FunctionHandler {
  public lateinit var name: String
  public open val scope: Scope? = null
  public abstract val minimumNumberOfArguments: Int?
  public abstract val maximumNumberOfArguments: Int?
  public var range: Range? = null

  protected abstract fun doFunction(argumentValues: List<Expression>, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType

  public fun executeFunction(arguments: List<Expression>, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    checkFunctionCall(arguments)
    val result = doFunction(arguments, editor, context, vimContext)
    range = null
    return result
  }

  private fun checkFunctionCall(arguments: List<Expression>) {
    if (minimumNumberOfArguments != null && arguments.size < minimumNumberOfArguments!!) {
      throw ExException("E119: Not enough arguments for function: $name")
    }
    if (maximumNumberOfArguments != null && arguments.size > maximumNumberOfArguments!!) {
      throw ExException("E118: Too many arguments for function: $name")
    }
  }
}
