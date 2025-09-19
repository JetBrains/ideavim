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
import com.maddyhome.idea.vim.ex.ranges.Range
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
    val function = FunctionDeclaration(
      null,
      getFunctionName(),
      args,
      listOf(),
      buildBody(),
      false,
      setOf(FunctionFlag.CLOSURE),
      true
    )
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
          Range(),
          Variable(Scope.LOCAL_VARIABLE, argument),
          AssignmentOperator.ASSIGNMENT,
          Variable(Scope.FUNCTION_VARIABLE, argument),
          true,
          "$argument = a:$argument"
        ),
      )
    }
    body.add(ReturnStatement(expr))
    return body
  }
}
