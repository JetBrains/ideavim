/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.varFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

@VimscriptFunction(name = "function")
internal class FunctionFunctionHandler : FunctionHandler() {
  override val minimumNumberOfArguments: Int = 1
  override val maximumNumberOfArguments: Int = 3

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimFuncref {
    val arg1 = argumentValues[0].evaluate(editor, context, vimContext)
    if (arg1 !is VimString) {
      throw exExceptionMessage("E129")
    }
    val scopeAndName = arg1.value.extractScopeAndName()
    val function =
      injector.functionService.getFunctionHandlerOrNull(scopeAndName.first, scopeAndName.second, vimContext)
        ?: throw exExceptionMessage("E700", (scopeAndName.first?.toString() ?: "") + scopeAndName.second)

    var arglist: VimList? = null
    var dictionary: VimDictionary? = null
    val arg2 = argumentValues.getOrNull(1)?.evaluate(editor, context, vimContext)
    val arg3 = argumentValues.getOrNull(2)?.evaluate(editor, context, vimContext)

    if (arg2 is VimDictionary && arg3 is VimDictionary) {
      throw exExceptionMessage("E923")
    }

    if (arg2 != null) {
      when (arg2) {
        is VimList -> arglist = arg2
        is VimDictionary -> dictionary = arg2
        else -> throw exExceptionMessage("E923")
      }
    }

    if (arg3 != null) {
      if (arg3 !is VimDictionary) {
        throw exExceptionMessage("E922")
      }
      dictionary = arg3
    }
    val funcref = VimFuncref(function, arglist ?: VimList(mutableListOf()), dictionary, VimFuncref.Type.FUNCTION)
    if (dictionary != null) {
      funcref.isSelfFixed = true
    }
    return funcref
  }
}

@VimscriptFunction(name = "funcref")
internal class FuncrefFunctionHandler : FunctionHandler() {
  override val minimumNumberOfArguments: Int = 1
  override val maximumNumberOfArguments: Int = 3

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimFuncref {
    val arg1 = argumentValues[0].evaluate(editor, context, vimContext)
    if (arg1 !is VimString) {
      throw exExceptionMessage("E129")
    }
    val scopeAndName = arg1.value.extractScopeAndName()
    val function = injector.functionService.getUserDefinedFunction(scopeAndName.first, scopeAndName.second, vimContext)
      ?: throw exExceptionMessage("E700", (scopeAndName.first?.toString() ?: "") + scopeAndName.second)
    val handler = DefinedFunctionHandler(function)

    var arglist: VimList? = null
    var dictionary: VimDictionary? = null
    val arg2 = argumentValues.getOrNull(1)?.evaluate(editor, context, vimContext)
    val arg3 = argumentValues.getOrNull(2)?.evaluate(editor, context, vimContext)

    if (arg2 is VimDictionary && arg3 is VimDictionary) {
      throw exExceptionMessage("E923")
    }

    if (arg2 != null) {
      when (arg2) {
        is VimList -> arglist = arg2
        is VimDictionary -> dictionary = arg2
        else -> throw exExceptionMessage("E923")
      }
    }

    if (arg3 != null) {
      if (arg3 !is VimDictionary) {
        throw exExceptionMessage("E922")
      }
      dictionary = arg3
    }
    return VimFuncref(handler, arglist ?: VimList(mutableListOf()), dictionary, VimFuncref.Type.FUNCREF)
  }
}

private fun String.extractScopeAndName(): Pair<Scope?, String> {
  val colonIndex = this.indexOf(":")
  if (colonIndex == -1) {
    return Pair(null, this)
  }
  val scopeString = this.substring(0, colonIndex)
  val nameString = this.substring(colonIndex + 1)
  return Pair(Scope.getByValue(scopeString), nameString)
}
