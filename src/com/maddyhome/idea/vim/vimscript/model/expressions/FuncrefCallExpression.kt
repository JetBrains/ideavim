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
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref

data class FuncrefCallExpression(val expression: Expression, val args: List<Expression>) : Expression() {

  fun evaluateWithRange(ranges: Ranges?, editor: Editor, context: DataContext, parent: Executable): VimDataType {
    val value = expression.evaluate(editor, context, parent)
    if (value is VimFuncref) {
      value.handler.ranges = ranges
      return value.execute(args, editor, context, parent)
    } else {
      // todo more exceptions
      throw ExException("E15: Invalid expression")
    }
  }

  override fun evaluate(editor: Editor, context: DataContext, parent: Executable): VimDataType {
    return evaluateWithRange(null, editor, context, parent)
  }
}
