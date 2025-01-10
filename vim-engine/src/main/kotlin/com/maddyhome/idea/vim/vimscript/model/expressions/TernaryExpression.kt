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

data class TernaryExpression(val condition: Expression, val then: Expression, val otherwise: Expression) :
  Expression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    return if (condition.evaluate(editor, context, vimContext).asDouble() != 0.0) {
      then.evaluate(editor, context, vimContext)
    } else {
      otherwise.evaluate(editor, context, vimContext)
    }
  }
}
