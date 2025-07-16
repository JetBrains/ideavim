/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi


import com.intellij.vim.api.Mode
import com.intellij.vim.api.scopes.CommandLineScope
import com.intellij.vim.api.scopes.DigraphScope
import com.intellij.vim.api.scopes.EditorScope
import com.intellij.vim.api.scopes.ListenersScope
import com.intellij.vim.api.scopes.MappingScope
import com.intellij.vim.api.scopes.ModalInput
import com.intellij.vim.api.scopes.OptionScope
import com.intellij.vim.api.scopes.OutputPanelScope
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.Key
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimOptionGroup
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.VimPluginContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.services.VariableService
import kotlinx.coroutines.runBlocking
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

  override suspend fun <T : Any> getVariableInternal(name: String, type: KType): T? {
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
    val value: T = injector.variableService.convertToKotlinType(variableValue, type)
    return value
  }

  override suspend fun setVariableInternal(name: String, value: Any, type: KType) {
    val (variableName, scope) = parseVariableName(name)
    val variableService: VariableService = injector.variableService
    val variable = Variable(scope, variableName)
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)

    val vimValue = variableService.convertToVimDataType(value, type)

    variableService.storeVariable(variable, vimValue, vimEditor, context, VimPluginContext)
  }

  override suspend fun lockvar(name: String, depth: Int) {
    val (variableName, scope) = parseVariableName(name)
    val variableService: VariableService = injector.variableService
    val variable = Variable(scope, variableName)

    variableService.lockVariable(variable, depth, vimEditor, vimContext, VimPluginContext)
  }

  override suspend fun unlockvar(name: String, depth: Int) {
    val (variableName, scope) = parseVariableName(name)
    val variableService: VariableService = injector.variableService
    val variable = Variable(scope, variableName)

    variableService.unlockVariable(variable, depth, vimEditor, vimContext, VimPluginContext)
  }

  override suspend fun islocked(name: String): Boolean {
    val (variableName, scope) = parseVariableName(name)
    val variableService: VariableService = injector.variableService
    val variable = Variable(scope, variableName)

    return variableService.isVariableLocked(variable, vimEditor, vimContext, VimPluginContext)
  }

  private fun parseVariableName(name: String): Pair<String, Scope?> {
    if (name.contains(':').not()) {
      return name to Scope.GLOBAL_VARIABLE
    }
    val prefix: String = name.substringBefore(':')
    val variableName: String = name.substringAfter(':')
    return variableName to Scope.getByValue(prefix)
  }

  override suspend fun exportOperatorFunction(name: String, function: suspend VimScope.() -> Boolean) {
    val operatorFunction: OperatorFunction = object : OperatorFunction {
      override fun apply(
        editor: VimEditor,
        context: ExecutionContext,
        selectionType: SelectionType?,
      ): Boolean {
        var returnValue = false
        injector.actionExecutor.executeCommand(vimEditor, {
          runBlocking {
            returnValue = VimScopeImpl(listenerOwner, mappingOwner).function()
          }
        }, "Insert Text", null)
        return returnValue
      }
    }
    injector.pluginService.exportOperatorFunction(name, operatorFunction)
  }

  override suspend fun setOperatorFunction(name: String) {
    injector.globalOptions().operatorfunc = name
  }

  override suspend fun normal(command: String) {
    injector.pluginService.executeNormalWithoutMapping(command, vimEditor)
  }

  override suspend fun editorScope(): EditorScope {
    return EditorScopeImpl(listenerOwner, mappingOwner)
  }

  override suspend fun <T> forEachEditor(block: suspend EditorScope.() -> T): List<T> {
    return injector.editorGroup.getEditors().map { editor ->
      val editorScope = EditorScopeImpl(listenerOwner, mappingOwner)
      editorScope.block()
    }
  }

  override suspend fun mappings(block: suspend MappingScope.() -> Unit) {
    val mappingScope = MappingScopeImpl(listenerOwner, mappingOwner)
    mappingScope.block()
  }

  override suspend fun listeners(block: suspend ListenersScope.() -> Unit) {
    val listenersScope = ListenerScopeImpl(listenerOwner, mappingOwner)
    listenersScope.block()
  }

  override suspend fun outputPanel(block: suspend OutputPanelScope.() -> Unit) {
    val outputPanelScope = OutputPanelScopeImpl()
    outputPanelScope.block()
  }

  override suspend fun modalInput(): ModalInput {
    return ModalInputImpl(listenerOwner, mappingOwner)
  }

  override suspend fun commandLine(block: suspend CommandLineScope.() -> Unit) {
    val commandLineScope = CommandLineScopeImpl(listenerOwner, mappingOwner)
    commandLineScope.block()
  }

  override suspend fun option(block: suspend OptionScope.() -> Unit) {
    val optionScope = OptionScopeImpl()
    optionScope.block()
  }

  override suspend fun digraph(block: suspend DigraphScope.() -> Unit) {
    val digraphScope = DigraphScopeImpl()
    digraphScope.block()
  }

  override val tabCount: Int
    get() = injector.tabService.getTabCount(vimContext)

  override val currentTabIndex: Int?
    get() = injector.tabService.getCurrentTabIndex(vimContext)

  override suspend fun removeTabAt(indexToDelete: Int, indexToSelect: Int) {
    injector.tabService.removeTabAt(indexToDelete, indexToSelect, vimContext)
  }

  override suspend fun moveCurrentTabToIndex(index: Int) {
    injector.tabService.moveCurrentTabToIndex(index, vimContext)
  }

  override suspend fun closeAllExceptCurrentTab() {
    injector.tabService.closeAllExceptCurrentTab(vimContext)
  }

  override suspend fun matches(pattern: String, text: String?, ignoreCase: Boolean): Boolean {
    return injector.regexpService.matches(pattern, text, ignoreCase)
  }

  override suspend fun getAllMatches(
    text: String,
    pattern: String,
  ): List<Pair<Int, Int>> {
    return injector.regexpService.getAllMatches(text, pattern)
  }

  override suspend fun selectNextWindow() {
    injector.window.selectNextWindow(vimContext)
  }

  override suspend fun selectWindow(index: Int) {
    injector.window.selectWindow(vimContext, index)
  }

  override suspend fun selectPreviousWindow() {
    injector.window.selectPreviousWindow(vimContext)
  }

  override suspend fun splitWindowVertically(filename: String?) {
    injector.window.splitWindowVertical(vimContext, filename ?: "")
  }

  override suspend fun splitWindowHorizontally(filename: String?) {
    injector.window.splitWindowHorizontal(vimContext, filename ?: "")
  }

  override suspend fun closeAllExceptCurrentWindow() {
    injector.window.closeAllExceptCurrent(vimContext)
  }

  override suspend fun closeCurrentWindow() {
    injector.window.closeCurrentWindow(vimContext)
  }

  override suspend fun closeAllWindows() {
    injector.window.closeAll(vimContext)
  }

  override suspend fun execute(script: String): Boolean {
    val result = injector.vimscriptExecutor.execute(
      script, vimEditor, vimContext,
      skipHistory = true,
      indicateErrors = true
    )
    return result == com.maddyhome.idea.vim.vimscript.model.ExecutionResult.Success
  }

  override suspend fun command(
    command: String,
    block: VimScope.(String) -> Unit,
  ) {
    val commandHandler = object : CommandAliasHandler {
      override fun execute(
        command: String,
        range: com.maddyhome.idea.vim.ex.ranges.Range,
        editor: VimEditor,
        context: ExecutionContext,
      ) {
        val vimScope = VimScopeImpl(listenerOwner, mappingOwner)
        vimScope.block(command)
      }
    }
    injector.pluginService.addCommand(command, commandHandler)
  }

  override suspend fun <T> getDataFromWindow(key: String): T? {
    val storageKey = Key<T>(key)
    return injector.vimStorageService.getDataFromWindow(vimEditor, storageKey)
  }

  override suspend fun <T> putDataToWindow(key: String, data: T) {
    val storageKey = Key<T>(key)
    injector.vimStorageService.putDataToWindow(vimEditor, storageKey, data)
  }

  override suspend fun <T> getDataFromBuffer(key: String): T? {
    val storageKey = Key<T>(key)
    return injector.vimStorageService.getDataFromBuffer(vimEditor, storageKey)
  }

  override suspend fun <T> putDataToBuffer(key: String, data: T) {
    val storageKey = Key<T>(key)
    injector.vimStorageService.putDataToBuffer(vimEditor, storageKey, data)
  }

  override suspend fun <T> getDataFromTab(key: String): T? {
    val storageKey = Key<T>(key)
    return injector.vimStorageService.getDataFromTab(vimEditor, storageKey)
  }

  override suspend fun <T> putDataToTab(key: String, data: T) {
    val storageKey = Key<T>(key)
    injector.vimStorageService.putDataToTab(vimEditor, storageKey, data)
  }

  override suspend fun saveFile() {
    injector.file.saveFile(vimEditor, vimContext)
  }

  override suspend fun closeFile() {
    injector.file.closeFile(vimEditor, vimContext)
  }

  override suspend fun getNextCamelStartOffset(chars: CharSequence, startIndex: Int, count: Int): Int? {
    return injector.searchHelper.findNextCamelStart(chars, startIndex, count)
  }

  override suspend fun getPreviousCamelStartOffset(chars: CharSequence, endIndex: Int, count: Int): Int? {
    return injector.searchHelper.findPreviousCamelStart(chars, endIndex, count)
  }

  override suspend fun getNextCamelEndOffset(chars: CharSequence, startIndex: Int, count: Int): Int? {
    return injector.searchHelper.findNextCamelEnd(chars, startIndex, count)
  }

  override suspend fun getPreviousCamelEndOffset(chars: CharSequence, endIndex: Int, count: Int): Int? {
    return injector.searchHelper.findPreviousCamelEnd(chars, endIndex, count)
  }

  override suspend fun getNextWordStartOffset(
    text: CharSequence,
    startOffset: Int,
    count: Int,
    isBigWord: Boolean,
  ): Int? {
    val editorSize = vimEditor.fileSize().toInt()
    val nextWordOffset = injector.searchHelper.findNextWord(text, vimEditor, startOffset, count, isBigWord)

    return if (nextWordOffset >= editorSize) {
      null
    } else {
      nextWordOffset
    }
  }
}
