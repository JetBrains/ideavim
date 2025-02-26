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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

data class SublistExpression(val from: Expression?, val to: Expression?, val expression: Expression) : Expression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val expressionValue = expression.evaluate(editor, context, vimContext)
    val arraySize = when (expressionValue) {
      is VimDictionary -> throw exExceptionMessage("E719")  // E719: Cannot slice a Dictionary
      is VimFuncref -> throw exExceptionMessage("E695") // E695: Cannot index a Funcref
      is VimList -> expressionValue.values.size
      else -> expressionValue.toVimString().value.length
    }
    var fromInt = from?.evaluate(editor, context, vimContext)?.toVimNumber()?.value ?: 0
    if (fromInt < 0) {
      fromInt += arraySize
    }
    var toInt = to?.evaluate(editor, context, vimContext)?.toVimNumber()?.value ?: (arraySize - 1)
    if (toInt < 0) {
      toInt += arraySize
    }
    return if (expressionValue is VimList) {
      if (fromInt > arraySize) {
        VimList(mutableListOf())
      } else if (fromInt == toInt) {
        expressionValue.values[fromInt] // TODO: This is incorrect, it should be a List
      } else if (fromInt <= toInt) {
        VimList(expressionValue.values.subList(fromInt, toInt + 1))
      } else {
        VimList(mutableListOf())
      }
    } else {
      if (fromInt > arraySize) {
        VimString.EMPTY
      } else if (fromInt <= toInt) {
        val string = expressionValue.toVimString().value
        if (toInt > string.length - 1) {
          VimString(string.substring(fromInt))
        } else {
          VimString(string.substring(fromInt, toInt + 1))
        }
      } else {
        VimString.EMPTY
      }
    }
  }
}
