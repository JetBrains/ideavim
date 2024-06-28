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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList

class FalsyExpression(val left: Expression, val right: Expression) : Expression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val leftValue = left.evaluate(editor, context, vimContext)
    val isLeftTrue = when (leftValue) {
      is VimList -> leftValue.values.isNotEmpty()
      is VimDictionary -> leftValue.dictionary.isNotEmpty()
      else -> leftValue.asBoolean()
    }
    return if (isLeftTrue) leftValue else right.evaluate(editor, context, vimContext)
  }
}
