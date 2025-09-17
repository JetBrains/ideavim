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
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

// expression[index]
// expression.index (entry in a dictionary)
data class IndexedExpression(val index: Expression, val expression: Expression) : Expression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val expressionValue = expression.evaluate(editor, context, vimContext)
    when (expressionValue) {
      is VimDictionary -> {
        val key = index.evaluate(editor, context, vimContext).toVimString()
        return expressionValue.dictionary[key]
          ?: throw exExceptionMessage("E716", key.toOutputString())
      }

      else -> {
        val indexValue = index.evaluate(editor, context, vimContext).toVimNumber().value
        // TODO: Support negative index to retrieve item from end
        if (expressionValue is VimList && (indexValue >= expressionValue.values.size || indexValue < 0)) {
          throw exExceptionMessage("E684", indexValue)
        }
        if (indexValue < 0) {
          return VimString.EMPTY
        }
        return SublistExpression(
          SimpleExpression(indexValue),
          SimpleExpression(indexValue),
          SimpleExpression(expressionValue)
        ).evaluate(editor, context, vimContext)
      }
    }
  }
}
