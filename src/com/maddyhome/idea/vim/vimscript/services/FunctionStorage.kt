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
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.functions.DefinedFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionBeanClass
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration

object FunctionStorage {

  private val builtInFunctionHandlers: MutableMap<String, FunctionHandler> = mutableMapOf()
  private val globalFunctionHandlers: MutableMap<String, FunctionHandler> = mutableMapOf()
  private val scriptFunctionHandlers: MutableMap<String, MutableMap<String, FunctionHandler>> = mutableMapOf()
  private val autoloadFunctionHandlers: MutableMap<String, FunctionHandler> = mutableMapOf()
  private val logger = logger<FunctionStorage>()
  private val extensionPoint = ExtensionPointName.create<FunctionBeanClass>("IdeaVIM.vimLibraryFunction")

  fun deleteFunction(name: String, vimContext: VimContext, scope: Scope? = null, scriptName: String? = null) {
    if (name[0].isLowerCase() && scope != Scope.SCRIPT_VARIABLE) {
      throw ExException("E128: Function name must start with a capital or \"s:\": $name")
    }

    if (scriptName != null) {
      val fullName = "$scriptName#$name"
      if (autoloadFunctionHandlers.containsKey(fullName)) {
        autoloadFunctionHandlers.remove(fullName)
        return
      } else {
        throw ExException("E130: Unknown function: $fullName")
      }
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
          if (scriptFunctionHandlers.containsKey(vimContext.getScriptName()) &&
            scriptFunctionHandlers[vimContext.getScriptName()]!!.containsKey(name)
          ) {
            scriptFunctionHandlers[vimContext.getScriptName()]!!.remove(name)
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
    if (scriptFunctionHandlers.containsKey(vimContext.getScriptName()) &&
      scriptFunctionHandlers[vimContext.getScriptName()]!!.containsKey(name)
    ) {
      scriptFunctionHandlers[vimContext.getScriptName()]!!.remove(name)
      return
    }
    throw ExException("E130: Unknown function: $name")
  }

  fun storeFunction(declaration: FunctionDeclaration, vimContext: VimContext) {
    if (declaration.scriptName != null) {
      val fullName = "${declaration.scriptName}#${declaration.name}"
      if (autoloadFunctionHandlers.containsKey(fullName) && !declaration.replaceExisting) {
        throw ExException("E122: Function $fullName already exists, add ! to replace it")
      }
      autoloadFunctionHandlers[fullName] = DefinedFunctionHandler(declaration.args, declaration.body)
      return
    }
    val scope: Scope = declaration.scope ?: getDefaultFunctionScope(vimContext)
    when (scope) {
      Scope.GLOBAL_VARIABLE -> {
        if (globalFunctionHandlers.containsKey(declaration.name) && !declaration.replaceExisting) {
          throw ExException("E122: Function ${declaration.name} already exists, add ! to replace it")
        } else {
          globalFunctionHandlers[declaration.name] = DefinedFunctionHandler(declaration.args, declaration.body)
        }
      }
      Scope.SCRIPT_VARIABLE -> {
        if (scriptFunctionHandlers.containsKey(vimContext.getScriptName()) &&
          scriptFunctionHandlers[vimContext.getScriptName()]!!.containsKey(declaration.name) &&
          !declaration.replaceExisting
        ) {
          throw ExException("E122: Function ${declaration.name} already exists, add ! to replace it")
        } else {
          if (scriptFunctionHandlers.containsKey(vimContext.getScriptName())) {
            scriptFunctionHandlers[vimContext.getScriptName()]!![declaration.name] =
              DefinedFunctionHandler(declaration.args, declaration.body)
          } else {
            scriptFunctionHandlers[vimContext.getScriptName()] =
              mutableMapOf(declaration.name to DefinedFunctionHandler(declaration.args, declaration.body))
          }
        }
      }
      else -> throw ExException("E884: Function name cannot contain a colon: ${scope.c}:${declaration.name}")
    }
  }

  fun getFunctionHandler(name: String, vimContext: VimContext, scope: Scope? = null, scriptName: String? = null):
    FunctionHandler {
    if (scriptName != null) {
      val fullName = "$scriptName#$name"
      return autoloadFunctionHandlers[fullName] ?: throw throw ExException("E117: Unknown function: $fullName")
    }
    if (builtInFunctionHandlers.containsKey(name)) {
      return builtInFunctionHandlers[name]!!
    }

    if (scope != null)
      when (scope) {
        Scope.GLOBAL_VARIABLE -> {
          if (globalFunctionHandlers.containsKey(name)) {
            return globalFunctionHandlers[name]!!
          } else {
            throw ExException("E117: Unknown function: ${scope.c}:$name")
          }
        }
        Scope.SCRIPT_VARIABLE -> {
          if (scriptFunctionHandlers.containsKey(vimContext.getScriptName()) &&
            scriptFunctionHandlers[vimContext.getScriptName()]!!.containsKey(name)
          ) {
            return scriptFunctionHandlers[vimContext.getScriptName()]!![name]!!
          } else {
            throw ExException("E117: Unknown function: ${scope.c}:$name")
          }
        }
        else -> throw ExException("E117: Unknown function: ${scope.c}:$name")
      }

    if (globalFunctionHandlers.containsKey(name)) {
      return globalFunctionHandlers[name]!!
    }
    if (scriptFunctionHandlers.containsKey(vimContext.getScriptName()) &&
      scriptFunctionHandlers[vimContext.getScriptName()]!!.containsKey(name)
    ) {
      return scriptFunctionHandlers[vimContext.getScriptName()]!![name]!!
    }
    throw ExException("E117: Unknown function: $name")
  }

  private fun getDefaultFunctionScope(vimContext: VimContext): Scope {
    return Scope.SCRIPT_VARIABLE
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
