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
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandlerBase
import com.maddyhome.idea.vim.vimscript.model.functions.VimscriptFunctionProvider
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag
import java.lang.ref.WeakReference

abstract class VimScriptFunctionServiceBase : VimscriptFunctionService {
  protected abstract val functionProviders: List<VimscriptFunctionProvider>

  private val globalFunctions: MutableMap<String, FunctionDeclaration> = mutableMapOf()
  private val builtInFunctions: MutableMap<String, Lazy<FunctionHandler>> = mutableMapOf()
  private val lambdaFunctions = mutableListOf<WeakReference<FunctionDeclaration>>()

  private var anonymousFunctionCounter = 1
  private var lambdaFunctionCounter = 1

  override fun deleteFunction(name: String, scope: Scope?, vimContext: VimLContext) {
    // Autoload names (containing '#') may start with a lowercase letter, like `foo#bar#baz`.
    if (name[0].isLowerCase() && !name.contains('#') && scope != Scope.SCRIPT_VARIABLE) {
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
    if (declaration.flags.contains(FunctionFlag.CLOSURE) && declaration.name.startsWith("<lambda>")) {
      // A lambda function is anonymous when created, but Vim gives it a unique name based on `<lambda>{counter}`. The
      // user can see this name with `:echo string(MyLambda)`. They are not typically treated as global functions, but
      // the `:function` command will list lambda functions when matching by name or pattern, so we need to store a
      // reference, as though it were a global function.
      // However, lambda functions are garbage collected when no longer referenced, for example, when a variable is
      // reassigned. IdeaVim supports this, but non-deterministically. Lambda functions are wrapped in a VimFuncref and
      // assigned to a variable. When the variable no longer references it, the JVM is free to GC it at some point in
      // the future.
      // Holding a weak reference allows us to report it in `:function` and let it be garbage collected, at the cost of
      // some incorrect reports before GC happens.
      lambdaFunctions.add(WeakReference(declaration))
      lambdaFunctions.removeAll { it.get() == null }
      return
    }

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

  override fun getAllUserDefinedFunctions() = globalFunctions.values + lambdaFunctions.mapNotNull { it.get() }

  override fun getBuiltInFunction(name: String): FunctionHandler? {
    return builtInFunctions[name]?.value
  }

  // TODO: This is incorrect. The correct API to add a function is storeFunction, and we shouldn't be adding built-ins!
  override fun registerFunctionHandler(
    functionName: String,
    functionHandler: FunctionHandler,
  ) {
    if (functionHandler is FunctionHandlerBase<*>) functionHandler.name = functionName
    builtInFunctions[functionName] = lazyOf(functionHandler)
  }

  // TODO: This is incorrect. We shouldn't have an API for removing a built-in function
  override fun unregisterFunctionHandler(functionName: String) {
    builtInFunctions.remove(functionName)
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
        builtInFunctions[it.name] = lazy { it.instance }
      }
    }
  }

  override fun getNextAnonymousFunctionName() = anonymousFunctionCounter++.toString()
  override fun getNextLambdaFunctionName() = "<lambda>" + lambdaFunctionCounter++

  override fun resetUserDefinedFunctions() {
    // Remove all global user-defined functions
    val iterator = globalFunctions.iterator()
    while (iterator.hasNext()) {
      val (_, function) = iterator.next()
      function.isDeleted = true
      iterator.remove()
    }

    // Note that this is a test-only function. We don't need to remove scoped functions, because the scopes will also
    // disappear

    anonymousFunctionCounter = 1
    lambdaFunctionCounter = 1

    // We don't need to mark lambda functions as deleted. They're typically Funcref objects that are garbage collected
    // when no longer referenced
    lambdaFunctions.clear()
  }
}
