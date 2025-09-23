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
import com.maddyhome.idea.vim.ex.exExceptionMessage
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
    val scopePrefix = scope?.toString() ?: ""
    val name = functionName.evaluate(editor, context, vimContext).value
    injector.statisticsService.setIfFunctionCallUsed(true)
    val handler = injector.functionService.getFunctionHandlerOrNull(scope, name, vimContext)
    if (handler != null) {
      if (handler is DefinedFunctionHandler && handler.function.flags.contains(FunctionFlag.DICT)) {
        throw exExceptionMessage("E725", scopePrefix + name)
      }
      return handler.executeFunction(this.arguments, editor, context, vimContext)
    }

    val funcref =
      injector.variableService.getNullableVariableValue(VariableExpression(scope, functionName), editor, context, vimContext)
    if (funcref is VimFuncref) {
      return funcref.execute(scopePrefix + name, arguments, editor, context, vimContext)
    }
    throw exExceptionMessage("E117", scopePrefix + name)
  }
}
