/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.vimscript.model.ExecutableContext
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

internal class VariableServiceImpl : VariableService {

  private var globalVariables: MutableMap<String, VimDataType> = mutableMapOf()
  private val tabAndWindowVariablesKey = Key<MutableMap<String, VimDataType>>("TabAndWindowVariables")
  private val bufferVariablesKey = Key<MutableMap<String, VimDataType>>("BufferVariables")

  private fun getDefaultVariableScope(executable: VimLContext): Scope {
    return when (executable.getExecutableContext(executable)) {
      ExecutableContext.SCRIPT, ExecutableContext.COMMAND_LINE -> Scope.GLOBAL_VARIABLE
      ExecutableContext.FUNCTION -> Scope.LOCAL_VARIABLE
    }
  }

  override fun isVariableLocked(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): Boolean {
    return getNullableVariableValue(variable, editor, context, vimContext)?.isLocked ?: false
  }

  override fun lockVariable(variable: Variable, depth: Int, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext) {
    val value = getNullableVariableValue(variable, editor, context, vimContext) ?: return
    value.lockOwner = variable
    value.lockVar(depth)
  }

  override fun unlockVariable(variable: Variable, depth: Int, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext) {
    val value = getNullableVariableValue(variable, editor, context, vimContext) ?: return
    value.unlockVar(depth)
  }

  override fun storeVariable(variable: Variable, value: VimDataType, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext) {
    val scope = variable.scope ?: getDefaultVariableScope(vimContext)
    when (scope) {
      Scope.GLOBAL_VARIABLE -> {
        storeGlobalVariable(variable.name.evaluate(editor, context, vimContext).value, value)
        val scopeForGlobalEnvironment = variable.scope?.toString() ?: ""
        VimScriptGlobalEnvironment.getInstance()
          .variables[scopeForGlobalEnvironment + variable.name.evaluate(editor, context, vimContext)] = value.simplify()
      }
      Scope.SCRIPT_VARIABLE -> storeScriptVariable(variable.name.evaluate(editor, context, vimContext).value, value, vimContext)
      Scope.WINDOW_VARIABLE, Scope.TABPAGE_VARIABLE -> {
        val variableKey = scope.c + ":" + variable.name
        if (editor.ij.getUserData(tabAndWindowVariablesKey) == null) {
          editor.ij.putUserData(tabAndWindowVariablesKey, mutableMapOf(variableKey to value))
        } else {
          editor.ij.getUserData(tabAndWindowVariablesKey)!![variableKey] = value
        }
      }
      Scope.FUNCTION_VARIABLE -> storeFunctionVariable(variable.name.evaluate(editor, context, vimContext).value, value, vimContext)
      Scope.LOCAL_VARIABLE -> storeLocalVariable(variable.name.evaluate(editor, context, vimContext).value, value, vimContext)
      Scope.BUFFER_VARIABLE -> {
        if (editor.ij.document.getUserData(bufferVariablesKey) == null) {
          editor.ij.document.putUserData(bufferVariablesKey, mutableMapOf(variable.name.evaluate(editor, context, vimContext).value to value))
        } else {
          editor.ij.document.getUserData(bufferVariablesKey)!![variable.name.evaluate(editor, context, vimContext).value] = value
        }
      }
      Scope.VIM_VARIABLE -> throw ExException("The 'v:' scope is not implemented yet :(")
    }
  }

  override fun getNullableVariableValue(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType? {
    val scope = variable.scope ?: getDefaultVariableScope(vimContext)
    return when (scope) {
      Scope.GLOBAL_VARIABLE -> getGlobalVariableValue(variable.name.evaluate(editor, context, vimContext).value)
      Scope.SCRIPT_VARIABLE -> getScriptVariable(variable.name.evaluate(editor, context, vimContext).value, vimContext)
      Scope.WINDOW_VARIABLE, Scope.TABPAGE_VARIABLE -> {
        val variableKey = scope.c + ":" + variable.name
        editor.ij.getUserData(tabAndWindowVariablesKey)?.get(variableKey)
      }
      Scope.FUNCTION_VARIABLE -> getFunctionVariable(variable.name.evaluate(editor, context, vimContext).value, vimContext)
      Scope.LOCAL_VARIABLE -> getLocalVariable(variable.name.evaluate(editor, context, vimContext).value, vimContext)
      Scope.BUFFER_VARIABLE -> {
        editor.ij.document.getUserData(bufferVariablesKey)?.get(variable.name.evaluate(editor, context, vimContext).value)
      }
      Scope.VIM_VARIABLE -> throw ExException("The 'v:' scope is not implemented yet :(")
    }
  }

  override fun getNonNullVariableValue(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    return getNullableVariableValue(variable, editor, context, vimContext)
      ?: throw ExException(
        "E121: Undefined variable: " +
          (if (variable.scope != null) variable.scope!!.c + ":" else "") +
          variable.name.evaluate(editor, context, vimContext).value
      )
  }

  override fun getGlobalVariableValue(name: String): VimDataType? {
    return globalVariables[name]
  }

  private fun getScriptVariable(name: String, vimContext: VimLContext): VimDataType? {
    val script = vimContext.getScript() ?: throw ExException("E121: Undefined variable: s:$name")
    return script.scriptVariables[name]
  }

  private fun getFunctionVariable(name: String, vimContext: VimLContext): VimDataType? {
    val visibleVariables = mutableListOf<Map<String, VimDataType>>()
    var node: VimLContext = vimContext
    while (!node.isFirstParentContext()) {
      if (node is FunctionDeclaration) {
        visibleVariables.add(node.functionVariables)
        if (!node.flags.contains(FunctionFlag.CLOSURE)) {
          break
        }
      }
      node = node.getPreviousParentContext()
    }

    visibleVariables.reverse()
    val functionVariablesMap = mutableMapOf<String, VimDataType>()
    for (map in visibleVariables) {
      functionVariablesMap.putAll(map)
    }
    return functionVariablesMap[name]
  }

  private fun getLocalVariable(name: String, vimContext: VimLContext): VimDataType? {
    val visibleVariables = mutableListOf<Map<String, VimDataType>>()
    var node: VimLContext = vimContext
    while (!node.isFirstParentContext()) {
      if (node is FunctionDeclaration) {
        visibleVariables.add(node.localVariables)
        if (!node.flags.contains(FunctionFlag.CLOSURE)) {
          break
        }
      }
      node = node.getPreviousParentContext()
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

  private fun storeScriptVariable(name: String, value: VimDataType, vimContext: VimLContext) {
    val script = vimContext.getScript() ?: throw ExException("E461: Illegal variable name: s:$name")
    script.scriptVariables[name] = value
  }

  private fun storeFunctionVariable(name: String, value: VimDataType, vimContext: VimLContext) {
    var node: VimLContext = vimContext
    while (!(node.isFirstParentContext() || node is FunctionDeclaration)) {
      node = node.getPreviousParentContext()
    }

    if (node is FunctionDeclaration) {
      node.functionVariables[name] = value
    } else {
      throw ExException("E461: Illegal variable name: a:$name")
    }
  }

  private fun storeLocalVariable(name: String, value: VimDataType, vimContext: VimLContext) {
    var node: VimLContext = vimContext
    while (!(node.isFirstParentContext() || node is FunctionDeclaration)) {
      node = node.getPreviousParentContext()
    }
    if (node is FunctionDeclaration) {
      node.localVariables[name] = value
    } else {
      throw ExException("E461: Illegal variable name: l:$name")
    }
  }

  fun clear() {
    globalVariables.clear()
  }

  private fun VimDataType.simplify(): Any {
    return when (this) {
      is VimString -> this.value
      is VimInt -> this.value
      is VimFloat -> this.value
      is VimList -> this.values
      is VimDictionary -> this.dictionary
      is VimBlob -> "blob"
      is VimFuncref -> "funcref"
      else -> error("Unexpected")
    }
  }
}
