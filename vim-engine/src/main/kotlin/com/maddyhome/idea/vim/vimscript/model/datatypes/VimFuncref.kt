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
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

data class VimFuncref(
  val handler: FunctionHandler,
  val arguments: VimList,
  var dictionary: VimDictionary?,
  val type: Type,
) : VimDataType() {

  var isSelfFixed: Boolean = false

  companion object {
    var lambdaCounter: Int = 1
    var anonymousCounter: Int = 1
  }

  override fun asDouble(): Double {
    throw exExceptionMessage("E703")  // E703: Using a Funcref as a Number
  }

  override fun toVimNumber(): VimInt {
    throw exExceptionMessage("E703")  // E703: Using a Funcref as a Number
  }

  override fun toVimString(): VimString {
    throw exExceptionMessage("E729")  // E729: Using a Funcref as a String
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
        throw ExException("E725: Calling dict function without Dictionary: $name")
      } else {
        injector.variableService.storeVariable(
          Variable(Scope.LOCAL_VARIABLE, "self"),
          dictionary!!,
          editor,
          context,
          handler.function,
        )
      }
    }

    val allArguments = listOf(this.arguments.values.map { SimpleExpression(it) }, args).flatten()
    if (handler is DefinedFunctionHandler && handler.function.isDeleted) {
      throw ExException("E933: Function was deleted: ${handler.name}")
    }
    val handler = when (type) {
      Type.LAMBDA, Type.FUNCREF -> this.handler
      Type.FUNCTION -> {
        injector.functionService.getFunctionHandlerOrNull(handler.scope, handler.name, vimContext)
          ?: throw ExException("E117: Unknown function: ${handler.name}")
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
