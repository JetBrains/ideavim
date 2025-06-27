/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi


import com.intellij.vim.api.Mode
import com.intellij.vim.api.scopes.EditorScope
import com.intellij.vim.api.scopes.ListenersScope
import com.intellij.vim.api.scopes.MappingScope
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimOptionGroup
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.VimPluginContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.services.VariableService
import kotlin.reflect.KType

open class VimScopeImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : VimScope() {
  override var mode: Mode
    get() {
      return injector.vimState.mode.toMode()
    }
    set(value) {
      changeMode(value, vimEditor)
    }

  private val vimEditor: VimEditor
    get() = injector.editorGroup.getFocusedEditor()!!

  private val vimContext: ExecutionContext
    get() = injector.executionContextManager.getEditorExecutionContext(vimEditor)

  private val optionGroup: VimOptionGroup
    get() = injector.optionGroup

  override fun <T : Any> getVariableInternal(name: String, type: KType): T? {
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

  override fun <T> getOptionValueInternal(name: String, type: KType): T? {
    val option = optionGroup.getOption(name) ?: return null

    val optionValue = optionGroup.getOptionValue(option, OptionAccessScope.EFFECTIVE(vimEditor))
    return parseOptionValue(optionValue, type)
  }

  override fun <T> setOptionInternal(name: String, value: T, type: KType, scope: String): Boolean {
    val option = optionGroup.getOption(name) ?: return false

    val optionValue = when (type.classifier) {
      Int::class -> {
        VimInt(value as Int)
      }

      String::class -> {
        VimString(value as String)
      }

      Boolean::class -> {
        if (value as Boolean) VimInt.ONE else VimInt.ZERO
      }

      else -> return false
    }
    val optionAccessScope = when (scope) {
      "global" -> OptionAccessScope.GLOBAL(vimEditor)
      "local" -> OptionAccessScope.LOCAL(vimEditor)
      "effective" -> OptionAccessScope.EFFECTIVE(vimEditor)
      else -> OptionAccessScope.EFFECTIVE(vimEditor)
    }
    optionGroup.setOptionValue(option, optionAccessScope, optionValue)
    return true
  }

  override fun resetOptionToDefault(name: String): Boolean {
    val option = optionGroup.getOption(name) ?: return false
    optionGroup.resetToDefaultValue(option, OptionAccessScope.EFFECTIVE(vimEditor))
    return true
  }

  override val tabCount: Int
    get() = injector.tabService.getTabCount(vimContext)

  override val currentTabIndex: Int?
    get() = injector.tabService.getCurrentTabIndex(vimContext)

  override fun removeTabAt(indexToDelete: Int, indexToSelect: Int) {
    injector.tabService.removeTabAt(indexToDelete, indexToSelect, vimContext)
  }

  override fun moveCurrentTabToIndex(index: Int) {
    injector.tabService.moveCurrentTabToIndex(index, vimContext)
  }

  override fun closeAllExceptCurrentTab() {
    injector.tabService.closeAllExceptCurrentTab(vimContext)
  }

  override fun matches(pattern: String, text: String?, ignoreCase: Boolean): Boolean {
    return injector.regexpService.matches(pattern, text, ignoreCase)
  }

  override fun getAllMatches(
    text: String,
    pattern: String,
  ): List<Pair<Int, Int>> {
    return injector.regexpService.getAllMatches(text, pattern)
  }

  override fun selectNextWindow() {
    injector.window.selectNextWindow(vimContext)
  }

  override fun selectWindow(index: Int) {
    injector.window.selectWindow(vimContext, index)
  }

  override fun selectPreviousWindow() {
    injector.window.selectPreviousWindow(vimContext)
  }

  override fun splitWindowVertically(filename: String?) {
    injector.window.splitWindowVertical(vimContext, filename ?: "")
  }

  override fun splitWindowHorizontally(filename: String?) {
    injector.window.splitWindowHorizontal(vimContext, filename ?: "")
  }

  override fun closeAllExceptCurrentWindow() {
    injector.window.closeAllExceptCurrent(vimContext)
  }

  override fun closeCurrentWindow() {
    injector.window.closeCurrentWindow(vimContext)
  }

  override fun closeAllWindows() {
    injector.window.closeAll(vimContext)
  }

  private fun <T : Any> parseOptionValue(vimDataType: VimDataType, type: KType): T? {
    return try {
      when (type.classifier) {
        Boolean::class -> vimDataType.asBoolean() as T
        Int::class -> vimDataType.toVimNumber().value as T
        String::class -> vimDataType.asString() as T
        List::class -> {
          if (vimDataType !is VimString) return null
          vimDataType.asString().split(",") as T
        }

        else -> null
      }
    } catch (e: Exception) {
      null
    }
  }
}
