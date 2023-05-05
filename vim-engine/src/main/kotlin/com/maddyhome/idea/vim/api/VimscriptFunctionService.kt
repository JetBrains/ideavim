/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.LazyVimscriptFunction
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration

public interface VimscriptFunctionService {

  public fun deleteFunction(name: String, scope: Scope? = null, vimContext: VimLContext)
  public fun storeFunction(declaration: FunctionDeclaration)
  public fun getFunctionHandler(scope: Scope?, name: String, vimContext: VimLContext): FunctionHandler
  public fun getFunctionHandlerOrNull(scope: Scope?, name: String, vimContext: VimLContext): FunctionHandler?
  public fun getUserDefinedFunction(scope: Scope?, name: String, vimContext: VimLContext): FunctionDeclaration?
  public fun getBuiltInFunction(name: String): FunctionHandler?
  public fun registerHandlers()
  public fun addHandler(handler: LazyVimscriptFunction)
}
