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
import com.maddyhome.idea.vim.vimscript.model.CurrentLocation
import com.maddyhome.idea.vim.vimscript.model.VimContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable

object VariableService {

  private var globalVariables: MutableMap<String, VimDataType> = mutableMapOf()
  private var scriptVariables: MutableMap<String, MutableMap<String, VimDataType>> = mutableMapOf()
  private val tabAndWindowVariablesKey = Key<MutableMap<String, VimDataType>>("TabAndWindowVariables")
  private val bufferVariablesKey = Key<MutableMap<String, VimDataType>>("BufferVariables")

  private fun getDefaultVariableScope(vimContext: VimContext): Scope {
    return when (vimContext.locations.peek()) {
      CurrentLocation.SCRIPT -> Scope.GLOBAL_VARIABLE
      CurrentLocation.FUNCTION -> Scope.LOCAL_VARIABLE
      null -> throw RuntimeException("VimContexts current location is undefined")
    }
  }

  fun storeVariable(
    variable: Variable,
    value: VimDataType,
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
  ) {
    val scope = variable.scope ?: getDefaultVariableScope(vimContext)
    when (scope) {
      Scope.GLOBAL_VARIABLE -> {
        globalVariables[variable.name] = value
        VimScriptGlobalEnvironment.getInstance().variables[(variable.scope?.c ?: "") + variable.name] = value.simplify()
      }
      Scope.SCRIPT_VARIABLE -> {
        val scriptName = vimContext.getScriptName()
        if (scriptVariables.containsKey(scriptName)) {
          scriptVariables[scriptName]?.set(variable.name, value)
        } else {
          scriptVariables[scriptName] = mutableMapOf(variable.name to value)
        }
      }
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
      Scope.FUNCTION_VARIABLE -> vimContext.functionVariables.peek()[variable.name] = value
      Scope.LOCAL_VARIABLE -> vimContext.localVariables.peek()[variable.name] = value
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
      Scope.VIM_VARIABLE -> TODO()
    }
  }

  fun getNullableVariableValue(
    variable: Variable,
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
  ): VimDataType? {
    val scope = variable.scope ?: getDefaultVariableScope(vimContext)
    return when (scope) {
      Scope.GLOBAL_VARIABLE -> globalVariables[variable.name]
      Scope.SCRIPT_VARIABLE -> scriptVariables[vimContext.getScriptName()]?.get(variable.name)
      Scope.WINDOW_VARIABLE, Scope.TABPAGE_VARIABLE -> {
        val variableKey = scope.c + ":" + variable.name
        if (editor != null) {
          editor.getUserData(tabAndWindowVariablesKey)?.get(variableKey)
        } else {
          TODO()
        }
      }
      Scope.FUNCTION_VARIABLE -> vimContext.functionVariables.peek()[variable.name]
      Scope.LOCAL_VARIABLE -> vimContext.localVariables.peek()[variable.name]
      Scope.BUFFER_VARIABLE -> {
        if (editor != null) {
          editor.document.getUserData(bufferVariablesKey)?.get(variable.name)
        } else {
          TODO()
        }
      }
      Scope.VIM_VARIABLE -> TODO()
    }
  }

  fun getNonNullVariableValue(
    variable: Variable,
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
  ): VimDataType {
    return getNullableVariableValue(variable, editor, context, vimContext)
      ?: throw ExException(
        "E121: Undefined variable: " +
          (if (variable.scope != null) variable.scope.c + ":" else "") +
          variable.name
      )
  }

  // todo fix me i'm ugly :(
  fun clear() {
    globalVariables = mutableMapOf()
    scriptVariables = mutableMapOf()
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
