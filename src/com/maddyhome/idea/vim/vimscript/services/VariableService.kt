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

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutableContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

object VariableService {

  private var globalVariables: MutableMap<String, VimDataType> = mutableMapOf()
  private val tabAndWindowVariablesKey = Key<MutableMap<String, VimDataType>>("TabAndWindowVariables")
  private val bufferVariablesKey = Key<MutableMap<String, VimDataType>>("BufferVariables")

  private fun getDefaultVariableScope(executable: Executable): Scope {
    return when (executable.getContext()) {
      ExecutableContext.SCRIPT -> Scope.GLOBAL_VARIABLE
      ExecutableContext.FUNCTION -> Scope.LOCAL_VARIABLE
    }
  }

  fun storeVariable(variable: Variable, value: VimDataType, editor: Editor?, context: DataContext?, parent: Executable) {
    val scope = variable.scope ?: getDefaultVariableScope(parent)
    when (scope) {
      Scope.GLOBAL_VARIABLE -> {
        storeGlobalVariable(variable.name, value)
        val scopeForGlobalEnvironment = if (variable.scope != null) variable.scope.c + ":" else ""
        VimScriptGlobalEnvironment.getInstance().variables[scopeForGlobalEnvironment + variable.name] = value.simplify()
      }
      Scope.SCRIPT_VARIABLE -> storeScriptVariable(variable.name, value, parent)
      Scope.WINDOW_VARIABLE, Scope.TABPAGE_VARIABLE -> {
        if (editor != null) {
          val variableKey = scope.c + ":" + variable.name
          if (editor.getUserData(tabAndWindowVariablesKey) == null) {
            editor.putUserData(tabAndWindowVariablesKey, mutableMapOf(variableKey to value))
          } else {
            editor.getUserData(tabAndWindowVariablesKey)!![variableKey] = value
          }
        } else {
          // todo nullable editor exception or something
        }
      }
      Scope.FUNCTION_VARIABLE -> storeFunctionVariable(variable.name, value, parent)
      Scope.LOCAL_VARIABLE -> storeLocalVariable(variable.name, value, parent)
      Scope.BUFFER_VARIABLE -> {
        if (editor != null) {
          if (editor.document.getUserData(bufferVariablesKey) == null) {
            editor.document.putUserData(bufferVariablesKey, mutableMapOf(variable.name to value))
          } else {
            editor.document.getUserData(bufferVariablesKey)!![variable.name] = value
          }
        } else {
          // todo nullable editor exception or something
        }
      }
      Scope.VIM_VARIABLE -> throw ExException("The 'v:' scope is not implemented yet :(")
    }
  }

  fun getNullableVariableValue(variable: Variable, editor: Editor?, context: DataContext?, parent: Executable): VimDataType? {
    val scope = variable.scope ?: getDefaultVariableScope(parent)
    return when (scope) {
      Scope.GLOBAL_VARIABLE -> getGlobalVariable(variable.name)
      Scope.SCRIPT_VARIABLE -> getScriptVariable(variable.name, parent)
      Scope.WINDOW_VARIABLE, Scope.TABPAGE_VARIABLE -> {
        val variableKey = scope.c + ":" + variable.name
        if (editor != null) {
          editor.getUserData(tabAndWindowVariablesKey)?.get(variableKey)
        } else {
          TODO()
        }
      }
      Scope.FUNCTION_VARIABLE -> getFunctionVariable(variable.name, parent)
      Scope.LOCAL_VARIABLE -> getLocalVariable(variable.name, parent)
      Scope.BUFFER_VARIABLE -> {
        if (editor != null) {
          editor.document.getUserData(bufferVariablesKey)?.get(variable.name)
        } else {
          TODO()
        }
      }
      Scope.VIM_VARIABLE -> throw ExException("The 'v:' scope is not implemented yet :(")
    }
  }

  fun getNonNullVariableValue(variable: Variable, editor: Editor?, context: DataContext?, parent: Executable): VimDataType {
    return getNullableVariableValue(variable, editor, context, parent)
      ?: throw ExException(
        "E121: Undefined variable: " +
          (if (variable.scope != null) variable.scope.c + ":" else "") +
          variable.name
      )
  }

  fun getGlobalVariable(name: String): VimDataType? {
    return globalVariables[name]
  }

  private fun getScriptVariable(name: String, parent: Executable): VimDataType? {
    val script = parent.getScript()
    return script.scriptVariables[name]
  }

  private fun getFunctionVariable(name: String, parent: Executable): VimDataType? {
    val visibleVariables = mutableListOf<Map<String, VimDataType>>()
    var node: Executable? = parent
    while (node != null) {
      if (node is FunctionDeclaration) {
        visibleVariables.add(node.functionVariables)
        if (!node.flags.contains(FunctionFlag.CLOSURE)) {
          break
        }
      }
      node = node.parent
    }

    visibleVariables.reverse()
    val functionVariablesMap = mutableMapOf<String, VimDataType>()
    for (map in visibleVariables) {
      functionVariablesMap.putAll(map)
    }
    return functionVariablesMap[name]
  }

  private fun getLocalVariable(name: String, parent: Executable): VimDataType? {
    val visibleVariables = mutableListOf<Map<String, VimDataType>>()
    var node: Executable? = parent
    while (node != null) {
      if (node is FunctionDeclaration) {
        visibleVariables.add(node.localVariables)
        if (!node.flags.contains(FunctionFlag.CLOSURE)) {
          break
        }
      }
      node = node.parent
    }

    visibleVariables.reverse()
    val localVariablesMap = mutableMapOf<String, VimDataType>()
    for (map in visibleVariables) {
      localVariablesMap.putAll(map)
    }
    return localVariablesMap[name]
  }

  fun storeGlobalVariable(name: String, value: VimDataType) {
    globalVariables[name] = value
  }

  private fun storeScriptVariable(name: String, value: VimDataType, parent: Executable) {
    val script = parent.getScript()
    script.scriptVariables[name] = value
  }

  private fun storeFunctionVariable(name: String, value: VimDataType, parent: Executable) {
    var node: Executable? = parent
    while (node != null) {
      if (node is FunctionDeclaration) {
        break
      }
      node = node.parent
    }
    if (node is FunctionDeclaration) {
      node.functionVariables[name] = value
    } else {
      TODO()
    }
  }

  private fun storeLocalVariable(name: String, value: VimDataType, parent: Executable) {
    var node: Executable? = parent
    while (node != null) {
      if (node is FunctionDeclaration) {
        break
      }
      node = node.parent
    }
    if (node is FunctionDeclaration) {
      node.localVariables[name] = value
    } else {
      TODO()
    }
  }

  // todo fix me i'm ugly :(
  fun clear() {
    globalVariables = mutableMapOf()
  }

  private fun VimDataType.simplify(): Any {
    return when (this) {
      is VimString -> this.value
      is VimInt -> this.value
      is VimFloat -> this.value
      is VimList -> this.values
      is VimDictionary -> this.dictionary
      is VimBlob -> throw NotImplementedError("Blobs are not implemented yet :(")
    }
  }
}
