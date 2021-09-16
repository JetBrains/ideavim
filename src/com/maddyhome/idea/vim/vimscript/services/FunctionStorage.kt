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

// todo create DeclaredFunction to replace DefinedFunctionHandler(functionDefinition)
// todo lots of refactoring
object FunctionStorage {

  private val logger = logger<FunctionStorage>()

  private val globalFunctionHandlers: MutableMap<String, FunctionHandler> = mutableMapOf()

  private val extensionPoint = ExtensionPointName.create<FunctionBeanClass>("IdeaVIM.vimLibraryFunction")
  private val builtInFunctionHandlers: MutableMap<String, FunctionHandler> = mutableMapOf()

  fun deleteFunction(name: String, scope: Scope? = null, parent: Executable) {
    if (name[0].isLowerCase() && scope != Scope.SCRIPT_VARIABLE) {
      throw ExException("E128: Function name must start with a capital or \"s:\": $name")
    }

    if (scope != null)
      when (scope) {
        Scope.GLOBAL_VARIABLE -> {
          if (globalFunctionHandlers.containsKey(name)) {
            globalFunctionHandlers.remove(name)
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

    if (globalFunctionHandlers.containsKey(name)) {
      globalFunctionHandlers.remove(name)
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
        if (globalFunctionHandlers.containsKey(declaration.name) && !declaration.replaceExisting) {
          throw ExException("E122: Function ${declaration.name} already exists, add ! to replace it")
        } else {
          globalFunctionHandlers[declaration.name] = DefinedFunctionHandler(declaration)
        }
      }
      Scope.SCRIPT_VARIABLE -> {
        if (getScriptFunction(declaration.name, declaration) != null && !declaration.replaceExisting) {
          throw ExException("E122: Function ${declaration.name} already exists, add ! to replace it")
        } else {
          storeScriptFunction(declaration.name, DefinedFunctionHandler(declaration), declaration)
        }
      }
      else -> throw ExException("E884: Function name cannot contain a colon: ${scope.c}:${declaration.name}")
    }
  }

  fun getFunctionHandler(name: String, scope: Scope? = null, parent: Executable):
    FunctionHandler {
      if (builtInFunctionHandlers.containsKey(name)) {
        return builtInFunctionHandlers[name]!!
      }

      if (scope != null)
        return when (scope) {
          Scope.GLOBAL_VARIABLE -> {
            if (globalFunctionHandlers.containsKey(name)) {
              globalFunctionHandlers[name]!!
            } else {
              throw ExException("E117: Unknown function: ${scope.c}:$name")
            }
          }
          Scope.SCRIPT_VARIABLE -> {
            getScriptFunction(name, parent) ?: throw ExException("E117: Unknown function: ${scope.c}:$name")
          }
          else -> throw ExException("E117: Unknown function: ${scope.c}:$name")
        }

      if (globalFunctionHandlers.containsKey(name)) {
        return globalFunctionHandlers[name]!!
      }
      val scriptFunctionHandler = getScriptFunction(name, parent)
      if (scriptFunctionHandler != null) {
        return scriptFunctionHandler
      }
      throw ExException("E117: Unknown function: $name")
    }

  private fun getDefaultFunctionScope(): Scope {
    return Scope.GLOBAL_VARIABLE // todd what is default scope?..
  }

  private fun deleteScriptFunction(name: String, parent: Executable) {
    val script = parent.getScript()
    script.scriptFunctions.remove(name)
  }

  private fun getScriptFunction(name: String, parent: Executable): FunctionHandler? {
    val script = parent.getScript()
    return script.scriptFunctions[name]
  }

  private fun storeScriptFunction(name: String, value: FunctionHandler, parent: Executable) {
    val script = parent.getScript()
    script.scriptFunctions[name] = value
  }

  fun registerHandlers() {
    extensionPoint.extensions().forEach(FunctionBeanClass::register)
  }

  fun addHandler(handlerHolder: FunctionBeanClass) {
    if (handlerHolder.name != null) {
      builtInFunctionHandlers[handlerHolder.name!!] = handlerHolder.instance
    } else {
      logger.error("Received function handler with null name")
    }
  }
}
