/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionBeanClass
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration

object FunctionStorage {

  private val logger = logger<FunctionStorage>()

  private val globalFunctions: MutableMap<String, FunctionDeclaration> = mutableMapOf()

  private val extensionPoint = ExtensionPointName.create<FunctionBeanClass>("IdeaVIM.vimLibraryFunction")
  private val builtInFunctions: MutableMap<String, FunctionHandler> = mutableMapOf()

  fun deleteFunction(name: String, scope: Scope? = null, parent: Executable) {
    if (name[0].isLowerCase() && scope != Scope.SCRIPT_VARIABLE) {
      throw ExException("E128: Function name must start with a capital or \"s:\": $name")
    }

    if (scope != null)
      when (scope) {
        Scope.GLOBAL_VARIABLE -> {
          if (globalFunctions.containsKey(name)) {
            globalFunctions[name]!!.isDeleted = true
            globalFunctions.remove(name)
            return
          } else {
            throw ExException("E130: Unknown function: ${scope.c}:$name")
          }
        }
        Scope.SCRIPT_VARIABLE -> {
          if (getScriptFunction(name, parent) != null) {
            deleteScriptFunction(name, parent)
            return
          } else {
            throw ExException("E130: Unknown function: ${scope.c}:$name")
          }
        }
        else -> throw ExException("E130: Unknown function: ${scope.c}:$name")
      }

    if (globalFunctions.containsKey(name)) {
      globalFunctions[name]!!.isDeleted = true
      globalFunctions.remove(name)
      return
    }
    if (getScriptFunction(name, parent) != null) {
      deleteScriptFunction(name, parent)
      return
    }
    throw ExException("E130: Unknown function: $name")
  }

  fun storeFunction(declaration: FunctionDeclaration) {
    val scope: Scope = declaration.scope ?: getDefaultFunctionScope()
    when (scope) {
      Scope.GLOBAL_VARIABLE -> {
        if (globalFunctions.containsKey(declaration.name) && !declaration.replaceExisting) {
          throw ExException("E122: Function ${declaration.name} already exists, add ! to replace it")
        } else {
          globalFunctions[declaration.name] = declaration
        }
      }
      Scope.SCRIPT_VARIABLE -> {
        if (getScriptFunction(declaration.name, declaration) != null && !declaration.replaceExisting) {
          throw ExException("E122: Function ${declaration.name} already exists, add ! to replace it")
        } else {
          storeScriptFunction(declaration)
        }
      }
      else -> throw ExException("E884: Function name cannot contain a colon: ${scope.c}:${declaration.name}")
    }
  }

  fun getFunctionHandler(scope: Scope?, name: String, parent: Executable): FunctionHandler {
    return getFunctionHandlerOrNull(scope, name, parent)
      ?: throw ExException("E117: Unknown function: ${if (scope != null) scope.c + ":" else ""}$name")
  }

  // todo g:abs should be unknown function !!!
  fun getFunctionHandlerOrNull(scope: Scope?, name: String, parent: Executable): FunctionHandler? {
    val builtInFunction = getBuiltInFunction(name)
    if (builtInFunction != null) {
      return builtInFunction
    }
    val definedFunction = getUserDefinedFunction(scope, name, parent)
    if (definedFunction != null) {
      return DefinedFunctionHandler(definedFunction)
    }
    return null
  }

  fun getUserDefinedFunction(scope: Scope?, name: String, parent: Executable): FunctionDeclaration? {
    return when (scope) {
      Scope.GLOBAL_VARIABLE -> globalFunctions[name]
      Scope.SCRIPT_VARIABLE -> getScriptFunction(name, parent)
      null -> globalFunctions[name] ?: getScriptFunction(name, parent)
      else -> null
    }
  }

  fun getBuiltInFunction(name: String): FunctionHandler? {
    return builtInFunctions[name]
  }

  private fun storeScriptFunction(functionDeclaration: FunctionDeclaration) {
    val script = functionDeclaration.getScript()
    script.scriptFunctions[functionDeclaration.name] = functionDeclaration
  }

  private fun getScriptFunction(name: String, parent: Executable): FunctionDeclaration? {
    val script = parent.getScript()
    return script.scriptFunctions[name]
  }

  private fun deleteScriptFunction(name: String, parent: Executable) {
    val script = parent.getScript()
    if (script.scriptFunctions[name] != null) {
      script.scriptFunctions[name]!!.isDeleted = true
    }
    script.scriptFunctions.remove(name)
  }

  private fun getDefaultFunctionScope(): Scope {
    return Scope.GLOBAL_VARIABLE
  }

  fun registerHandlers() {
    extensionPoint.extensions().forEach(FunctionBeanClass::register)
  }

  fun addHandler(handlerHolder: FunctionBeanClass) {
    if (handlerHolder.name != null) {
      builtInFunctions[handlerHolder.name!!] = handlerHolder.instance
    } else {
      logger.error("Received function handler with null name")
    }
  }
}
