/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref

public data class FuncrefCallExpression(val expression: Expression, val args: List<Expression>) : Expression() {

  public fun evaluateWithRange(range: Range?, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val value = expression.evaluate(editor, context, vimContext)
    if (value is VimFuncref) {
      value.handler.range = range
      return value.execute(value.handler.name, args, editor, context, vimContext)
    } else {
      // todo more exceptions
      throw ExException("E15: Invalid expression")
    }
  }

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    return evaluateWithRange(null, editor, context, vimContext)
  }
}
