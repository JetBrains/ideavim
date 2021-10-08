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

package com.maddyhome.idea.vim.vimscript.model.statements

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.OneElementSublistExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler

data class AnonymousFunctionDeclaration(
  val sublist: OneElementSublistExpression,
  val args: List<String>,
  val defaultArgs: List<Pair<String, Expression>>,
  val body: List<Executable>,
  val replaceExisting: Boolean,
  val flags: Set<FunctionFlag>,
  val hasOptionalArguments: Boolean,
) : Executable {

  override lateinit var parent: Executable

  override fun execute(editor: Editor, context: DataContext): ExecutionResult {
    val container = sublist.expression.evaluate(editor, context, parent)
    if (container !is VimDictionary) {
      throw ExException("E1203: Dot can only be used on a dictionary")
    }
    val index = ((sublist.index as SimpleExpression).data as VimString)
    if (container.dictionary.containsKey(index)) {
      if (container.dictionary[index] is VimFuncref && !replaceExisting) {
        throw ExException("E717: Dictionary entry already exists")
      } else if (container.dictionary[index] !is VimFuncref) {
        throw ExException("E718: Funcref required")
      }
    }
    val declaration = FunctionDeclaration(null, VimFuncref.anonymousCounter++.toString(), args, defaultArgs, body, replaceExisting, flags + FunctionFlag.DICT, hasOptionalArguments)
    container.dictionary[index] = VimFuncref(DefinedFunctionHandler(declaration), VimList(mutableListOf()), container, VimFuncref.Type.FUNCREF)
    return ExecutionResult.Success
  }
}
