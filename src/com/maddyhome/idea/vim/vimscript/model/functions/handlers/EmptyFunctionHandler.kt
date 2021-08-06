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

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

object EmptyFunctionHandler : FunctionHandler() {

  override val minimumNumberOfArguments: Int = 1
  override val maximumNumberOfArguments: Int = 1

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: Editor,
    context: DataContext,
    vimContext: VimContext,
  ): VimDataType {
    val argument = argumentValues[0].evaluate(editor, context, vimContext)
    // TODO: 03.08.2021
    // - |v:false|, |v:none| and |v:null| are empty, |v:true| is not.
    val isEmpty = when (argument) {
      is VimList -> argument.values.isEmpty()
      is VimDictionary -> argument.dictionary.isEmpty()
      is VimString -> argument.value.isEmpty()
      is VimInt -> argument.value == 0
      is VimFloat -> argument.value == 0.0
      is VimBlob -> TODO("Not yet implemented")
    }
    return isEmpty.asVimInt()
  }
}
