/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.VariableScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimPluginContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import com.maddyhome.idea.vim.vimscript.services.VariableService
import kotlin.reflect.KType

class VariableScopeImpl(
  private val vimEditor: VimEditor,
  private val vimContext: ExecutionContext,
) : VariableScope {
  override fun <T : Any> getVariable(name: String, type: KType): T? {
    val (variableName, scope) = parseVariableName(name)
    val variableService: VariableService = injector.variableService

    val variableValue: VimDataType? = if (scope == Scope.GLOBAL_VARIABLE) {
      variableService.getGlobalVariableValue(variableName)
    } else {
      val variable = VariableExpression(scope, variableName)
      val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
      variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext)
    }

    if (variableValue == null) {
      return variableValue
    }
    val value: T = injector.variableService.convertToKotlinType(variableValue, type)
    return value
  }

  override fun setVariable(name: String, value: Any, type: KType) {
    val (variableName, scope) = parseVariableName(name)
    val variableService: VariableService = injector.variableService
    val variable = VariableExpression(scope, variableName)

    val isLocked = variableService.isVariableLocked(variable, vimEditor, vimContext, VimPluginContext)
    if (isLocked) {
      throw exExceptionMessage("E741", name)
    }

    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val vimValue = variableService.convertToVimDataType(value, type)
    variableService.storeVariable(variable, vimValue, vimEditor, context, VimPluginContext)
  }

  private fun parseVariableName(name: String): Pair<String, Scope?> {
    if (name.contains(':').not()) {
      return name to Scope.GLOBAL_VARIABLE
    }
    val prefix: String = name.substringBefore(':')
    val variableName: String = name.substringAfter(':')
    return variableName to Scope.getByValue(prefix)
  }
}
