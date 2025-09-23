/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

data class VimFuncref(
  val handler: FunctionHandler,
  val arguments: VimList,
  var dictionary: VimDictionary?,
  val type: Type,
) : VimDataType("funcref") {
  var isSelfFixed: Boolean = false

  companion object {
    var lambdaCounter: Int = 1
    var anonymousCounter: Int = 1
  }

  override fun toVimFloat(): VimFloat {
    throw exExceptionMessage("E891")
  }

  override fun toVimNumber(): VimInt {
    throw exExceptionMessage("E703")
  }

  override fun toVimString(): VimString {
    throw exExceptionMessage("E729")
  }

  override fun toOutputString(): String {
    return if (arguments.values.isEmpty() && dictionary == null) {
      when (type) {
        Type.LAMBDA -> "function('${handler.name}')"
        Type.FUNCREF -> "function('${handler.name}')"
        Type.FUNCTION -> handler.name
      }
    } else {
      val result = StringBuffer("function('${handler.name}'")
      if (arguments.values.isNotEmpty()) {
        result.append(", ").append(arguments.toOutputString())
      }
      result.append(")")
      return result.toString()
    }
  }

  fun execute(
    name: String,
    args: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    if (handler is DefinedFunctionHandler && handler.function.flags.contains(FunctionFlag.DICT)) {
      if (dictionary == null) {
        throw exExceptionMessage("E725", name)
      } else {
        injector.variableService.storeVariable(
          VariableExpression(Scope.LOCAL_VARIABLE, "self"),
          dictionary!!,
          editor,
          context,
          handler.function,
        )
      }
    }

    val allArguments = listOf(this.arguments.values.map { SimpleExpression(it) }, args).flatten()
    if (handler is DefinedFunctionHandler && handler.function.isDeleted) {
      throw exExceptionMessage("E933", handler.name)
    }
    val handler = when (type) {
      Type.LAMBDA, Type.FUNCREF -> this.handler
      Type.FUNCTION -> {
        injector.functionService.getFunctionHandlerOrNull(handler.scope, handler.name, vimContext)
          ?: throw exExceptionMessage("E117", handler.name)
      }
    }
    return handler.executeFunction(allArguments, editor, context, vimContext)
  }

  override fun deepCopy(level: Int): VimFuncref {
    return copy()
  }

  override fun lockVar(depth: Int) {
    this.isLocked = true
  }

  override fun unlockVar(depth: Int) {
    this.isLocked = false
  }

  enum class Type {
    LAMBDA,
    FUNCREF,
    FUNCTION,
  }
}
