/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi


import com.intellij.vim.api.Color
import com.intellij.vim.api.Mode
import com.intellij.vim.api.TextSelectionType
import com.intellij.vim.api.scopes.EditorScope
import com.intellij.vim.api.scopes.ListenersScope
import com.intellij.vim.api.scopes.MappingScope
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.selectionType
import com.maddyhome.idea.vim.vimscript.model.VimPluginContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.services.VariableService
import kotlin.reflect.KType
import java.awt.Color as AwtColor

open class VimScopeImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : VimScope {
  override var mode: Mode
    get() {
      return injector.vimState.mode.toMode()
    }
    set(value) {
      changeMode(value, vimEditor)
    }

  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  override fun getSelectionTypeForCurrentMode(): TextSelectionType? {
    val typeInEditor = injector.vimState.mode.selectionType ?: return null
    return typeInEditor.toTextSelectionType()
  }

  override fun <T : Any> getVariable(name: String, type: KType): T? {
    val (name, scope) = parseVariableName(name)
    val variableService: VariableService = injector.variableService
    val variable = Variable(scope, name)
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val variableValue: VimDataType? =
      variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext)
    if (variableValue == null) {
      return variableValue
//      throw IllegalArgumentException("Variable with name $name does not exist")
    }
    val value: T = parseVariableValue(variableValue, type)
    return value
  }

  private fun <T : Any> parseVariableValue(vimDataType: VimDataType, type: KType): T {
    return injector.variableService.parseVariableValue(vimDataType, type)
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
        return VimScopeImpl(listenerOwner, mappingOwner).function()
      }
    }
    injector.pluginService.exportOperatorFunction(name, operatorFunction)
  }

  override fun setOperatorFunction(name: String) {
    injector.globalOptions().operatorfunc = name
  }

  override fun normal(command: String) {
    injector.pluginService.executeNormalWithoutMapping(command, vimEditor)
  }

  override fun editor(block: EditorScope.() -> Unit) {
    val editorScope = EditorScopeImpl(listenerOwner, mappingOwner)
    editorScope.block()
  }

  override fun mappings(block: MappingScope.() -> Unit) {
    val mappingScope = MappingScopeImpl(listenerOwner, mappingOwner)
    mappingScope.block()
  }

  override fun listeners(block: ListenersScope.() -> Unit) {
    val listenersScope = ListenerScopeImpl(listenerOwner, mappingOwner)
    listenersScope.block()
  }

  override fun parseRgbaColor(rgbaString: String): Color? {
    // todo: replace with regex
    val rgba = rgbaString.removePrefix("rgba(")
      .filter { it != '(' && it != ')' && !it.isWhitespace() }
      .split(',')
      .map { it.toInt() }

    if (rgba.size != 4 || rgba.any { it < 0 || it > 255 }) {
      throw IllegalArgumentException("Invalid RGBA values. Each component must be between 0 and 255")
    }

    val awtColor = AwtColor(rgba[0], rgba[1], rgba[2], rgba[3])
    return awtColor.toHexColor()
  }
}