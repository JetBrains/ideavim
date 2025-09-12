/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi


import com.intellij.vim.api.VimApi
import com.intellij.vim.api.models.Mode
import com.intellij.vim.api.models.Path
import com.intellij.vim.api.scopes.DigraphScope
import com.intellij.vim.api.scopes.MappingScope
import com.intellij.vim.api.scopes.ModalInput
import com.intellij.vim.api.scopes.OptionScope
import com.intellij.vim.api.scopes.OutputPanelScope
import com.intellij.vim.api.scopes.commandline.CommandLineScope
import com.intellij.vim.api.scopes.editor.EditorScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.Key
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.thinapi.commandline.CommandLineScopeImpl
import com.maddyhome.idea.vim.thinapi.editor.EditorScopeImpl
import com.maddyhome.idea.vim.vimscript.model.VimPluginContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.services.VariableService
import kotlinx.coroutines.runBlocking
import kotlin.io.path.pathString
import kotlin.reflect.KType

class VimApiImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : VimApi {
  // Note: Setting a new mode is a complicated operation. Currently, it updates the selection under the write lock,
  //   but we don't require to run this under the write lock. Also, esc in insert mode may produce more inserts
  //   when the insert was started with the number: `3iabc<esc>`
  // It'll be necessary to review how the set of mode should be presented in the API.
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

  override fun <T : Any> getVariable(name: String, type: KType): T? {
    val (name, scope) = parseVariableName(name)
    val variableService: VariableService = injector.variableService
    val variable = Variable(scope, name)
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val variableValue: VimDataType? =
      variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext)
    // variable does not exist
    if (variableValue == null) {
      return variableValue
    }
    val value: T = injector.variableService.convertToKotlinType(variableValue, type)
    return value
  }

  override fun setVariable(name: String, value: Any, type: KType) {
    val (variableName, scope) = parseVariableName(name)
    val variableService: VariableService = injector.variableService
    val variable = Variable(scope, variableName)

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

  override fun exportOperatorFunction(name: String, function: suspend VimApi.() -> Boolean) {
    val operatorFunction: OperatorFunction = object : OperatorFunction {
      override fun apply(
        editor: VimEditor,
        context: ExecutionContext,
        selectionType: SelectionType?,
      ): Boolean {
        var returnValue = false
        injector.actionExecutor.executeCommand(vimEditor, {
          runBlocking {
            returnValue = VimApiImpl(listenerOwner, mappingOwner).function()
          }
        }, "Insert Text", null)
        return returnValue
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

  override fun <T> editor(block: EditorScope.() -> T): T {
    return EditorScopeImpl().block()
  }

  override fun <T> forEachEditor(block: EditorScope.() -> T): List<T> {
    return injector.editorGroup.getEditors().map { editor ->
      val editorScope = EditorScopeImpl()
      editorScope.block()
    }
  }

  override fun mappings(block: MappingScope.() -> Unit) {
    val mappingScope = MappingScopeImpl(listenerOwner, mappingOwner)
    mappingScope.block()
  }

//  override fun listeners(block: ListenersScope.() -> Unit) {
//    val listenersScope = ListenerScopeImpl(listenerOwner, mappingOwner)
//    listenersScope.block()
//  }

  override fun outputPanel(block: OutputPanelScope.() -> Unit) {
    val outputPanelScope = OutputPanelScopeImpl
    outputPanelScope.block()
  }

  override fun modalInput(): ModalInput {
    return ModalInputImpl(listenerOwner, mappingOwner)
  }

  override fun commandLine(block: CommandLineScope.() -> Unit) {
    val commandLineScope = CommandLineScopeImpl(listenerOwner, mappingOwner)
    commandLineScope.block()
  }

  override fun <T> option(block: OptionScope.() -> T): T {
    return OptionScopeImpl.block()
  }

  override fun digraph(block: DigraphScope.() -> Unit) {
    DigraphScopeImpl.block()
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

  override fun matches(pattern: String, text: String, ignoreCase: Boolean): Boolean {
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

  override fun splitWindowVertically(filePath: Path?) {
    injector.window.splitWindowVertical(vimContext, filePath?.javaPath?.pathString ?: "")
  }

  override fun splitWindowHorizontally(filePath: Path?) {
    injector.window.splitWindowHorizontal(vimContext, filePath?.javaPath?.pathString ?: "")
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

  override fun execute(script: String): Boolean {
    val result = injector.vimscriptExecutor.execute(
      script, vimEditor, vimContext,
      skipHistory = true,
      indicateErrors = true
    )
    return result == com.maddyhome.idea.vim.vimscript.model.ExecutionResult.Success
  }

  override fun command(
    command: String,
    block: VimApi.(String) -> Unit,
  ) {
    val commandHandler = object : CommandAliasHandler {
      override fun execute(
        command: String,
        range: com.maddyhome.idea.vim.ex.ranges.Range,
        editor: VimEditor,
        context: ExecutionContext,
      ) {
        val vimApi = VimApiImpl(listenerOwner, mappingOwner)
        vimApi.block(command)
      }
    }
    injector.pluginService.addCommand(command, commandHandler)
  }

  override fun <T> getDataFromWindow(key: String): T? {
    val storageKey = Key<T>(key)
    return injector.vimStorageService.getDataFromWindow(vimEditor, storageKey)
  }

  override fun <T> putDataToWindow(key: String, data: T) {
    val storageKey = Key<T>(key)
    injector.vimStorageService.putDataToWindow(vimEditor, storageKey, data)
  }

  override fun <T> getDataFromBuffer(key: String): T? {
    val storageKey = Key<T>(key)
    return injector.vimStorageService.getDataFromBuffer(vimEditor, storageKey)
  }

  override fun <T> putDataToBuffer(key: String, data: T) {
    val storageKey = Key<T>(key)
    injector.vimStorageService.putDataToBuffer(vimEditor, storageKey, data)
  }

  override fun <T> getDataFromTab(key: String): T? {
    val storageKey = Key<T>(key)
    return injector.vimStorageService.getDataFromTab(vimEditor, storageKey)
  }

  override fun <T> putDataToTab(key: String, data: T) {
    val storageKey = Key<T>(key)
    injector.vimStorageService.putDataToTab(vimEditor, storageKey, data)
  }

  override fun saveFile() {
    injector.file.saveFile(vimEditor, vimContext)
  }

  override fun closeFile() {
    injector.file.closeFile(vimEditor, vimContext)
  }

  override fun getNextCamelStartOffset(chars: CharSequence, startIndex: Int, count: Int): Int? {
    return injector.searchHelper.findNextCamelStart(chars, startIndex, count)
  }

  override fun getPreviousCamelStartOffset(chars: CharSequence, endIndex: Int, count: Int): Int? {
    return injector.searchHelper.findPreviousCamelStart(chars, endIndex, count)
  }

  override fun getNextCamelEndOffset(chars: CharSequence, startIndex: Int, count: Int): Int? {
    return injector.searchHelper.findNextCamelEnd(chars, startIndex, count)
  }

  override fun getPreviousCamelEndOffset(chars: CharSequence, endIndex: Int, count: Int): Int? {
    return injector.searchHelper.findPreviousCamelEnd(chars, endIndex, count)
  }
}
