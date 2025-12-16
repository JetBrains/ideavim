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
 * when the function is invoked. Note that a Funcref can be "implicitly" partial. This is not described in the Vim docs,
 * but applies to dictionary functions. A dictionary entry might be a Funcref, but when accessed with e.g. `dict.func`,
 * the returned value is evaluated to be an (implicitly) partial Funcref, with the owning dictionary passed to the new
 * Funcref. If this Funcref is called, the partially applied dictionary is used as `self`. If the Funcref is assigned to
 * a new dictionary, it keeps the implicitly partially applied dictionary but will create a new implicit partial when
 * the entry is evaluated. If the user creates a partially applied Funcref with a dictionary, this stored value always
 * takes precedence.
 *
 * @param handler The function handler to use when executing the function
 * @param arguments The arguments to use when executing the function
 * @param dictionary The dictionary to use when executing the function
 * @param type The type of the Funcref
 * @param isImplicitPartial True when the Funcref is partially applied when evaluating lookup on a dictionary function
 */
class VimFuncref(
  val handler: FunctionHandler,
  val arguments: VimList,
  val dictionary: VimDictionary?,
  val type: Type,
  val isImplicitPartial: Boolean
) : VimDataType("funcref") {
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

  override fun toOutputString() = buildString {
    val visited = mutableSetOf<VimDataType>()
    buildOutputString(this, visited)
  }

  override fun buildOutputString(builder: StringBuilder, visited: MutableSet<VimDataType>) {
    builder.run {
      // When outputting a Function, we output only the name if it's the only item being output. If it's part of a List
      // or Dictionary, we use the `function('...')` format
      if (type == Type.FUNCTION && arguments.values.isEmpty() && dictionary == null && visited.isEmpty()) {
        append(handler.name)
      } else {
        visited.add(this@VimFuncref)
        append("function('${handler.name}'")
        if (arguments.values.isNotEmpty()) {
          append(", ")
          arguments.buildOutputString(this, visited.toMutableSet())
        }
        if (dictionary != null) {
          append(", ")
          dictionary.buildOutputString(this, visited.toMutableSet())
        }
        append(")")
      }
    }
  }

  override fun toInsertableString(): String = throw exExceptionMessage("E729")

  override fun valueEquals(other: VimDataType, ignoreCase: Boolean, depth: Int): Boolean {
    if (this === other) return true
    if (other !is VimFuncref) return false
    if (handler.name != other.handler.name) return false
    if (!arguments.valueEquals(other.arguments, ignoreCase, depth)) return false
    val thisDictionary = this.dictionary
    val otherDictionary = other.dictionary
    when {
      thisDictionary == null && otherDictionary != null -> return false
      thisDictionary != null && otherDictionary == null -> return false
      thisDictionary != null && otherDictionary != null -> {
        if (!thisDictionary.valueEquals(otherDictionary, ignoreCase, depth + 1)) return false
      }
    }
    return true
  }

  /**
   * Execute the function with the given arguments
   *
   * If the Funcref is partially applied, the given arguments are concatenated to the existing arguments. If the
   * function handler is a dictionary function, the Funcref must be a partially applied Funcref with a dictionary, or
   * this method will throw E725. Note that accessing a dictionary entry (e.g. `dict.func`) that is a function will
   * evaluate it to a partially applied Funcref.
   */
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
          dictionary,
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

  override fun copy() = VimFuncref(handler, arguments.copy(), dictionary?.copy(), type, isImplicitPartial)

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
