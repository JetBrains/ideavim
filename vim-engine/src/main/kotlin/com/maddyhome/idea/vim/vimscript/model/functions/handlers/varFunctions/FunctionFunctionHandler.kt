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

    val arg2 = argumentValues.getOrNull(1)?.evaluate(editor, context, vimContext)
    val arg3 = argumentValues.getOrNull(2)?.evaluate(editor, context, vimContext)

    val (arglist, dictionary) = parseArglistAndDictionary(arg2, arg3)

    val funcref = VimFuncref(function, arglist, dictionary, VimFuncref.Type.FUNCTION)
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

    val arg2 = argumentValues.getOrNull(1)?.evaluate(editor, context, vimContext)
    val arg3 = argumentValues.getOrNull(2)?.evaluate(editor, context, vimContext)

    val (arglist, dictionary) = parseArglistAndDictionary(arg2, arg3)

    return VimFuncref(handler, arglist, dictionary, VimFuncref.Type.FUNCREF)
  }
}

/**
 * Parses and validates the arglist and dictionary arguments for function() and funcref().
 *
 * @param arg2 The second argument, which can be a VimList (arglist) or VimDictionary (dictionary)
 * @param arg3 The third argument, which must be a VimDictionary if present
 * @return A pair of (arglist, dictionary) where arglist defaults to empty list if not provided
 * @throws ExException E923 if arg2 is not a list or dict, or if both arg2 and arg3 are dicts
 * @throws ExException E922 if arg3 is not a dict
 */
private fun parseArglistAndDictionary(arg2: Any?, arg3: Any?): Pair<VimList, VimDictionary?> {
  // Check if both arg2 and arg3 are dictionaries (not allowed)
  if (arg2 is VimDictionary && arg3 is VimDictionary) {
    throw exExceptionMessage("E923")
  }

  var arglist: VimList? = null
  var dictionary: VimDictionary? = null

  // Parse arg2: can be either a list (arglist) or a dictionary
  if (arg2 != null) {
    when (arg2) {
      is VimList -> arglist = arg2
      is VimDictionary -> dictionary = arg2
      else -> throw exExceptionMessage("E923")
    }
  }

  // Parse arg3: must be a dictionary if present
  if (arg3 != null) {
    if (arg3 !is VimDictionary) {
      throw exExceptionMessage("E922")
    }
    dictionary = arg3
  }

  return Pair(arglist ?: VimList(mutableListOf()), dictionary)
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
