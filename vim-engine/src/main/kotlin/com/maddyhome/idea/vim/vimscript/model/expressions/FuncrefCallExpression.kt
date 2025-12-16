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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref

/**
 * Represents a function call through an expression that resolves to a Funcref, e.g. `foo[12]()`, `(expr)()` or `dict.foo()`
 *
 * The expression will not be a [VariableExpression]. This is handled by [NamedFunctionCallExpression], which can handle
 * a (Funcref) variable name, a simple name or a curly braces name.
 */
data class FuncrefCallExpression(val expression: Expression, val args: List<Expression>) : Expression() {

  fun evaluateWithRange(
    range: Range?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val value = expression.evaluate(editor, context, vimContext)
    if (value is VimFuncref) {
      return value.execute(value.handler.name, args, range, editor, context, vimContext)
    } else {
      // todo more exceptions
      throw exExceptionMessage("E15", expression.originalString)
    }
  }

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    return evaluateWithRange(null, editor, context, vimContext)
  }
}
