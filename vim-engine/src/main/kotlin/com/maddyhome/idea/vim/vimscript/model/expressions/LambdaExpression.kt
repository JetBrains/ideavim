/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.commands.LetCommand
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.AssignmentOperator
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag
import com.maddyhome.idea.vim.vimscript.model.statements.ReturnStatement

data class LambdaExpression(val args: List<String>, val expr: Expression) : Expression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimFuncref {
    val function = FunctionDeclaration(null, getFunctionName(), args, listOf(), buildBody(), false, setOf(FunctionFlag.CLOSURE), true)
    function.vimContext = vimContext
    return VimFuncref(DefinedFunctionHandler(function), VimList(mutableListOf()), null, VimFuncref.Type.LAMBDA)
  }

  private fun getFunctionName(): String {
    return "<lambda>" + VimFuncref.lambdaCounter++
  }

  private fun buildBody(): List<Executable> {
    val body = mutableListOf<Executable>()
    for (argument in args) {
      body.add(
        LetCommand(
          Ranges(), Variable(Scope.LOCAL_VARIABLE, argument), AssignmentOperator.ASSIGNMENT,
          Variable(
            Scope.FUNCTION_VARIABLE, argument
          ),
          true
        )
      )
    }
    body.add(ReturnStatement(expr))
    return body
  }
}
