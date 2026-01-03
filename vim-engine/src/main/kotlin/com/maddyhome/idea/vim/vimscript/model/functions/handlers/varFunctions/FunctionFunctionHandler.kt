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
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

@VimscriptFunction(name = "function")
internal class FunctionFunctionHandler : FunctionFunctionHandlerBase(VimFuncref.Type.FUNCTION) {
  override fun getFunctionHandler(scope: Scope?, name: String, vimContext: VimLContext): FunctionHandler? {
    return injector.functionService.getFunctionHandlerOrNull(scope, name, vimContext)
  }
}

@VimscriptFunction(name = "funcref")
internal class FuncrefFunctionHandler : FunctionFunctionHandlerBase(VimFuncref.Type.FUNCREF) {
  override fun getFunctionHandler(scope: Scope?, name: String, vimContext: VimLContext): FunctionHandler? {
    val declaration = injector.functionService.getUserDefinedFunction(scope, name, vimContext)
    return if (declaration != null) DefinedFunctionHandler(declaration) else null
  }
}


internal abstract class FunctionFunctionHandlerBase(private val funcrefType: VimFuncref.Type) :
  BuiltinFunctionHandler<VimFuncref>(minArity = 1, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext
  ): VimFuncref {
    val arg1 = arguments[0]
    if (arg1 !is VimString) {
      throw exExceptionMessage("E129")
    }

    val scopeAndName = Scope.split(arg1.value)
    val function = getFunctionHandler(scopeAndName.first, scopeAndName.second, vimContext)
      ?: throw exExceptionMessage("E700", (scopeAndName.first?.toString() ?: "") + scopeAndName.second)

    val arg2 = arguments.getOrNull(1)
    val arg3 = arguments.getOrNull(2)

    val argList = arg2 as? VimList

    val dictionary = if (arg2 != null && arg2 !is VimList) {
      if (arg2 !is VimDictionary) {
        throw exExceptionMessage("E923")
      }
      if (arg3 != null) {
        // If arg2 is a dictionary, arg3 must not be specific. Vim gives a slightly unintuitive error message
        throw exExceptionMessage("E1206", 3)
      }
      arg2
    }
    else if (arg3 != null) {
      if (arg3 !is VimDictionary) {
        throw exExceptionMessage("E1206", 3)
      }
      arg3
    }
    else {
      null
    }

    return VimFuncref(function, argList ?: VimList(mutableListOf()), dictionary, funcrefType, isImplicitPartial = false)
  }

  protected abstract fun getFunctionHandler(scope: Scope?, name: String, vimContext: VimLContext): FunctionHandler?
}
