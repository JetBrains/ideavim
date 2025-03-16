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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

class FalsyExpression(val left: Expression, val right: Expression) : Expression() {
  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val leftValue = left.evaluate(editor, context, vimContext)
    return if (isTruthy(leftValue)) leftValue else right.evaluate(editor, context, vimContext)
  }
}

data class TernaryExpression(val condition: Expression, val then: Expression, val otherwise: Expression) :
  Expression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext) =
    if (isTruthy(condition.evaluate(editor, context, vimContext))) {
      then.evaluate(editor, context, vimContext)
    } else {
      otherwise.evaluate(editor, context, vimContext)
    }
}

/**
 * Is the expression result "truthy" or "falsy"?
 *
 * Ternary expressions (`x ? y : z`) and "falsy" expressions (`x ?? y`) don't check for a boolean Number value, but
 * check to see if the expression is "sort of true" or "sort of false".
 *
 * See `:help truthy`
 */
private fun isTruthy(value: VimDataType) = when (value) {
  is VimInt -> value.value != 0
  is VimFloat -> value.value != 0.0
  is VimString -> value.value.isNotEmpty() && value.value != "0"
  is VimList -> value.values.isNotEmpty()
  is VimDictionary -> value.dictionary.isNotEmpty()
  is VimBlob -> TODO("Not yet implemented")
  else -> false
}
