/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.CommandLineVimLContext
import com.maddyhome.idea.vim.vimscript.model.Script
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.LazyVimscriptFunction
import com.maddyhome.idea.vim.vimscript.model.functions.VimscriptFunctionProvider
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration

abstract class VimScriptFunctionServiceBase : VimscriptFunctionService {
  protected abstract val functionProviders: List<VimscriptFunctionProvider>

  private val globalFunctions: MutableMap<String, FunctionDeclaration> = mutableMapOf()
  private val builtInFunctions: MutableMap<String, LazyVimscriptFunction> = mutableMapOf()

  override fun deleteFunction(name: String, scope: Scope?, vimContext: VimLContext) {
    if (name[0].isLowerCase() && scope != Scope.SCRIPT_VARIABLE) {
      throw exExceptionMessage("E128", name)
    }

    if (scope != null) {
      when (scope) {
        Scope.GLOBAL_VARIABLE -> {
          if (globalFunctions.containsKey(name)) {
            globalFunctions[name]!!.isDeleted = true
            globalFunctions.remove(name)
            return
          } else {
            throw exExceptionMessage("E130", scope.toString() + name)
          }
        }

        Scope.SCRIPT_VARIABLE -> {
          if (vimContext.getFirstParentContext() !is Script) {
            throw exExceptionMessage("E81")
          }

          if (getScriptFunction(name, vimContext) != null) {
            deleteScriptFunction(name, vimContext)
            return
          } else {
            throw exExceptionMessage("E130", scope.toString() + name)
          }
        }

        else -> throw exExceptionMessage("E130", scope.toString() + name)
      }
    }

    if (globalFunctions.containsKey(name)) {
      globalFunctions[name]!!.isDeleted = true
      globalFunctions.remove(name)
      return
    }

    val firstParentContext = vimContext.getFirstParentContext()
    if (firstParentContext is Script && getScriptFunction(name, vimContext) != null) {
      deleteScriptFunction(name, vimContext)
      return
    }
    throw exExceptionMessage("E130", name)
  }

  override fun storeFunction(declaration: FunctionDeclaration) {
    val scope: Scope = declaration.scope ?: getDefaultFunctionScope()
    when (scope) {
      Scope.GLOBAL_VARIABLE -> {
        if (globalFunctions.containsKey(declaration.name) && !declaration.replaceExisting) {
          throw exExceptionMessage("E122", declaration.name)
        } else {
          globalFunctions[declaration.name] = declaration
        }
      }

      Scope.SCRIPT_VARIABLE -> {
        if (declaration.getFirstParentContext() !is Script) {
          throw exExceptionMessage("E81")
        }

        if (getScriptFunction(declaration.name, declaration) != null && !declaration.replaceExisting) {
          throw exExceptionMessage("E122", declaration.name)
        } else {
          storeScriptFunction(declaration)
        }
      }

      else -> throw exExceptionMessage("E884", scope.toString() + declaration.name)
    }
  }

  override fun getFunctionHandler(scope: Scope?, name: String, vimContext: VimLContext): FunctionHandler {
    return getFunctionHandlerOrNull(scope, name, vimContext)
      ?: throw exExceptionMessage("E117", "${scope?.toString() ?: ""}$name")
  }

  override fun getFunctionHandlerOrNull(scope: Scope?, name: String, vimContext: VimLContext): FunctionHandler? {
    if (scope == null) {
      val builtInFunction = getBuiltInFunction(name)
      if (builtInFunction != null) {
        return builtInFunction
      }
    }

    val definedFunction = getUserDefinedFunction(scope, name, vimContext)
    if (definedFunction != null) {
      return DefinedFunctionHandler(definedFunction)
    }
    return null
  }

  override fun getUserDefinedFunction(scope: Scope?, name: String, vimContext: VimLContext): FunctionDeclaration? {
    return when (scope) {
      Scope.GLOBAL_VARIABLE -> globalFunctions[name]
      Scope.SCRIPT_VARIABLE -> getScriptFunction(name, vimContext)
      null -> {
        val firstParentContext = vimContext.getFirstParentContext()
        when (firstParentContext) {
          is CommandLineVimLContext -> globalFunctions[name]
          is Script -> globalFunctions[name] ?: getScriptFunction(name, vimContext)
          else -> throw RuntimeException("Unknown parent context")
        }
      }

      else -> null
    }
  }

  override fun getBuiltInFunction(name: String): FunctionHandler? {
    return builtInFunctions[name]?.instance
  }

  private fun storeScriptFunction(functionDeclaration: FunctionDeclaration) {
    val script = functionDeclaration.getScript() ?: throw exExceptionMessage("E81")
    script.scriptFunctions[functionDeclaration.name] = functionDeclaration
  }

  private fun getScriptFunction(name: String, vimContext: VimLContext): FunctionDeclaration? {
    val script = vimContext.getScript() ?: throw exExceptionMessage("E120", name)
    return script.scriptFunctions[name]
  }

  private fun deleteScriptFunction(name: String, vimContext: VimLContext) {
    val script = vimContext.getScript() ?: throw exExceptionMessage("E81")
    if (script.scriptFunctions[name] != null) {
      script.scriptFunctions[name]!!.isDeleted = true
    }
    script.scriptFunctions.remove(name)
  }

  private fun getDefaultFunctionScope(): Scope {
    return Scope.GLOBAL_VARIABLE
  }

  override fun registerHandlers() {
    functionProviders.forEach { provider ->
      provider.getFunctions().forEach {
        builtInFunctions[it.name] = it
      }
    }
  }

  override fun resetUserDefinedFunctions() {
    // Remove all global user-defined functions
    val iterator = globalFunctions.iterator()
    while (iterator.hasNext()) {
      val (_, function) = iterator.next()
      function.isDeleted = true
      iterator.remove()
    }

    // TODO: How to remove scoped functions?
  }
}
