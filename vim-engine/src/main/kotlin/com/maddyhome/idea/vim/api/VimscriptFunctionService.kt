package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration

interface VimscriptFunctionService {

  fun deleteFunction(name: String, scope: Scope? = null, vimContext: VimLContext)
  fun storeFunction(declaration: FunctionDeclaration)
  fun getFunctionHandler(scope: Scope?, name: String, vimContext: VimLContext): FunctionHandler
  fun getFunctionHandlerOrNull(scope: Scope?, name: String, vimContext: VimLContext): FunctionHandler?
  fun getUserDefinedFunction(scope: Scope?, name: String, vimContext: VimLContext): FunctionDeclaration?
  fun getBuiltInFunction(name: String): FunctionHandler?
  fun registerHandlers()
  fun addHandler(handlerHolder: Any)
}
