/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

data class OneElementSublistExpression(val index: Expression, val expression: Expression) : Expression() {

  override fun evaluate(editor: Editor, context: DataContext, vimContext: VimContext): VimDataType {
    val expressionValue = expression.evaluate(editor, context, vimContext)
    if (expressionValue is VimDictionary) {
      return expressionValue.dictionary[VimString(index.evaluate(editor, context, vimContext).asString())]
        ?: throw ExException(
          "E716: Key not present in Dictionary: \"${
          index.evaluate(editor, context, vimContext).asString()
          }\""
        )
    } else {
      val indexValue = Integer.parseInt(index.evaluate(editor, context, vimContext).asString())
      if (expressionValue is VimList && (indexValue >= expressionValue.values.size || indexValue < expressionValue.values.size)) {
        throw ExException("E684: list index out of range: $indexValue")
      }
      if (indexValue < 0) {
        return VimString("")
      }
      return SublistExpression(SimpleExpression(VimInt(indexValue)), SimpleExpression(VimInt(indexValue)), SimpleExpression(expressionValue)).evaluate(editor, context, vimContext)
    }
  }
}
