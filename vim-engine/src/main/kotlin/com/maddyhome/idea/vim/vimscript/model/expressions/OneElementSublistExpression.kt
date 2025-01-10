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
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

data class OneElementSublistExpression(val index: Expression, val expression: Expression) : Expression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val expressionValue = expression.evaluate(editor, context, vimContext)
    if (expressionValue is VimDictionary) {
      return expressionValue.dictionary[VimString(index.evaluate(editor, context, vimContext).asString())]
        ?: throw ExException(
          "E716: Key not present in Dictionary: \"${
            index.evaluate(editor, context, vimContext).asString()
          }\"",
        )
    } else {
      val indexValue = Integer.parseInt(index.evaluate(editor, context, vimContext).asString())
      if (expressionValue is VimList && (indexValue >= expressionValue.values.size || indexValue < 0)) {
        throw ExException("E684: list index out of range: $indexValue")
      }
      if (indexValue < 0) {
        return VimString("")
      }
      return SublistExpression(
        SimpleExpression(indexValue),
        SimpleExpression(indexValue),
        SimpleExpression(expressionValue)
      ).evaluate(editor, context, vimContext)
    }
  }
}
