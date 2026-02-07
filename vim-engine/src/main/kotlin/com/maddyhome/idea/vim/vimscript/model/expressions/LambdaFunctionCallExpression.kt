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
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

/**
 * Represents a lambda declaration being used as a function call
 *
 * This expression is in the form `{ arg1, arg2, ... -> body } (args)`
 */
class LambdaFunctionCallExpression(val lambda: LambdaExpression, val arguments: List<Expression>) : Expression() {
  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val funcref = lambda.evaluate(editor, context, vimContext)
    return funcref.execute(arguments, range = null, editor, context, vimContext)
  }
}
