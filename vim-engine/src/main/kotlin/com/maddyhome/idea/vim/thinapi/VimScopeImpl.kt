/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi


import com.intellij.vim.api.Mode
import com.intellij.vim.api.TextSelectionType
import com.intellij.vim.api.scopes.EditorScope
import com.intellij.vim.api.scopes.MappingScope
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.impl.state.toMappingMode
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.selectionType
import com.maddyhome.idea.vim.vimscript.model.VimPluginContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.services.VariableService

open class VimScopeImpl(
  private val editor: VimEditor,
  private val context: ExecutionContext,
) : VimScope {
  override var mode: Mode
    get() {
      return editor.mode.toMappingMode().toMode()
    }
    set(value) {
      // a lot of custom logic
    }

  override fun getSelectionTypeForCurrentMode(): TextSelectionType? {
    val typeInEditor = editor.mode.selectionType ?: return null
    return typeInEditor.toTextSelectionType()
  }

  override fun getVariableInt(name: String): Int? {
    val (name, scope) = parseVariableName(name)
    val variableService: VariableService = injector.variableService
    val variable = Variable(scope, name)
    val variableValue: VimDataType =
      variableService.getNullableVariableValue(variable, editor, context, VimPluginContext) ?: return null
    val intValue: Int? = (variableValue as? VimInt)?.value
    return intValue
  }

  private fun parseVariableName(name: String): Pair<String, Scope?> {
    if (name.contains(':').not()) {
      return name to Scope.GLOBAL_VARIABLE
    }
    val prefix: String = name.substringBefore(':')
    val variableName: String = name.substringAfter(':')
    return variableName to Scope.getByValue(prefix)
  }

  override fun exportOperatorFunction(name: String, function: VimScope.() -> Boolean) {
    val operatorFunction: OperatorFunction = object : OperatorFunction {
      override fun apply(
        editor: VimEditor,
        context: ExecutionContext,
        selectionType: SelectionType?,
      ): Boolean {
        return VimScopeImpl(editor, context).function()
      }
    }
    injector.pluginService.exportOperatorFunction(name, operatorFunction)
  }

  override fun setOperatorFunction(name: String) {
    injector.globalOptions().operatorfunc = name
  }

  override fun normal(command: String) {
    injector.pluginService.executeNormalWithoutMapping(command, editor)
  }

  // todo: Probably we don't need it
  override fun exitVisualMode() {
    editor.exitVisualMode()
  }

  override fun editor(block: EditorScope.() -> Unit) {
    val editorScope = EditorScopeImpl(editor, context)
    editorScope.block()
  }

  override fun mappings(block: MappingScope.() -> Unit) {
    val mappingScope = MappingScopeImpl()
    mappingScope.block()
  }
}