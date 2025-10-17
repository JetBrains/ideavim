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

/**
 * Represents a Vim Funcref, or reference to the function
 *
 * This type does NOT have value semantics. It is not correct to compare two instances of this type for structural
 * equality. This is required so that recursive data structures don't cause problems with equality or hash codes.
 *
 * It cannot be converted to a Number, Float, or String. When output, any recursively used elements are replaced with a
 * placeholder. When attempting to insert into text, an exception is thrown: "E729: Using a Funcref as a String".
 *
 * A Funcref can be early bound, late bound or a lambda, and can be partially applied.
 *
 * When created with Vim's `function()` function, it is late bound. The Funcref is created with a function handler for
 * validation purposes, but the handler is resolved at execution time, allowing for redefinition of the function. When
 * created with `funcref()`, it is early bound, and the handler given at creation time is used at execution time.
 *
 * A lambda is a Funcref that represents a lambda literal expression.
 *
 * If the Funcref is created with arguments and/or a dictionary, it is partially applied, and these values are used
 * when the function is invoked.
 *
 * Use [execute] to invoke the function. This will resolve the function handler if necessary and ensure a dictionary
 * function has the dictionary scope it requires. Do not invoke the function handler directly!
 */
class VimFuncref(
  val handler: FunctionHandler,
  val arguments: VimList,
  var dictionary: VimDictionary?,
  val type: Type,
) : VimDataType("funcref") {
  // TODO: Consider removing. It is set when the funcref is a partial and used to avoid overwriting the dictionary when invoked
  // It might be better to have a nullable dictionary for partial application, and pass the dictionary context when
  // invoking the function
  var isSelfFixed: Boolean = false

  companion object {
    var lambdaCounter: Int = 1
    var anonymousCounter: Int = 1
  }

  val isPartial: Boolean
    get() = arguments.values.isNotEmpty() || dictionary != null

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
      result.toString()
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
    // TODO: Confirm semantics
    return VimFuncref(handler, arguments.deepCopy(0), dictionary?.deepCopy(0), type)
  }

  override fun lockVar(depth: Int) {
    this.isLocked = true
  }

  override fun unlockVar(depth: Int) {
    this.isLocked = false
  }

  enum class Type {
    /**
     * An early bound function, referenced directly
     *
     * The function reference is early bound, with the funcref storing a direct reference to the function. If the
     * function is redefined, the funcref will still call the original function.
     *
     * The function reference can be a partial, either with arguments or bound to a dictionary or both.
     */
    FUNCREF,

    /**
     * A late bound function, referenced by name
     *
     * The function reference is late bound, with the function looked up by name at evaluation time. This means the
     * function reference will still work even if the function has been redefined.
     *
     * The function reference can be a partial, either with arguments or bound to a dictionary or both.
     */
    FUNCTION,

    /**
     * An early bound reference to a lambda expression
     */
    LAMBDA,

  }
}
