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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

data class FunctionCallExpression(
  val scope: Scope?,
  val functionName: CurlyBracesName,
  val arguments: MutableList<Expression>,
) :
  Expression() {
  constructor(scope: Scope?, functionName: String, arguments: MutableList<Expression>) :
    this(scope, CurlyBracesName(listOf(SimpleExpression(functionName))), arguments)

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    injector.statisticsService.setIfFunctionCallUsed(true)
    val handler = injector.functionService.getFunctionHandlerOrNull(
      scope,
      functionName.evaluate(editor, context, vimContext).value,
      vimContext
    )
    if (handler != null) {
      if (handler is DefinedFunctionHandler && handler.function.flags.contains(FunctionFlag.DICT)) {
        throw ExException(
          "E725: Calling dict function without Dictionary: " +
            (scope?.toString() ?: "") + functionName.evaluate(editor, context, vimContext),
        )
      }
      return handler.executeFunction(this.arguments, editor, context, vimContext)
    }

    val funcref =
      injector.variableService.getNullableVariableValue(Variable(scope, functionName), editor, context, vimContext)
    if (funcref is VimFuncref) {
      val name = (if (scope != null) scope.c + ":" else "") + functionName
      return funcref.execute(name, arguments, editor, context, vimContext)
    }
    throw ExException(
      "E117: Unknown function: ${if (scope != null) scope.c + ":" else ""}${
        functionName.evaluate(
          editor,
          context,
          vimContext
        )
      }"
    )
  }
}
