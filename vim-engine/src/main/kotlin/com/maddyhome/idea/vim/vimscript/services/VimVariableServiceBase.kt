package com.maddyhome.idea.vim.vimscript.services

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.Key
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.ExecutableContext
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag

abstract class VimVariableServiceBase : VariableService {
  private var globalVariables: MutableMap<String, VimDataType> = mutableMapOf()
  private val windowVariablesKey = Key<MutableMap<String, VimDataType>>("TabVariables")
  private val bufferVariablesKey = Key<MutableMap<String, VimDataType>>("BufferVariables")
  private val tabVariablesKey = Key<MutableMap<String, VimDataType>>("WindowVariables")

  private fun getWindowVariables(editor: VimEditor): MutableMap<String, VimDataType> {
    val storedVariableMap = injector.vimStorageService.getDataFromEditor(editor, windowVariablesKey)
    if (storedVariableMap != null) {
      return storedVariableMap
    }
    val windowVariables = mutableMapOf<String, VimDataType>()
    injector.vimStorageService.putDataToEditor(editor, windowVariablesKey, windowVariables)
    return windowVariables
  }

  private fun getBufferVariables(editor: VimEditor): MutableMap<String, VimDataType> {
    val storedVariableMap = injector.vimStorageService.getDataFromBuffer(editor, bufferVariablesKey)
    if (storedVariableMap != null) {
      return storedVariableMap
    }
    val bufferVariables = mutableMapOf<String, VimDataType>()
    injector.vimStorageService.putDataToBuffer(editor, bufferVariablesKey, bufferVariables)
    return bufferVariables
  }

  private fun getTabVariables(editor: VimEditor): MutableMap<String, VimDataType> {
    val storedVariableMap = injector.vimStorageService.getDataFromTab(editor, tabVariablesKey)
    if (storedVariableMap != null) {
      return storedVariableMap
    }
    val tabVariables = mutableMapOf<String, VimDataType>()
    injector.vimStorageService.putDataToTab(editor, tabVariablesKey, tabVariables)
    return tabVariables
  }

  protected fun getDefaultVariableScope(executable: VimLContext): Scope {
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

  override fun getGlobalVariables(): Map<String, VimDataType> {
    return globalVariables
  }

  override fun storeVariable(variable: Variable, value: VimDataType, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext) {
    val scope = variable.scope ?: getDefaultVariableScope(vimContext)
    val name = variable.name.evaluate(editor, context, vimContext).value
    when (scope) {
      Scope.GLOBAL_VARIABLE -> storeGlobalVariable(name, value)
      Scope.SCRIPT_VARIABLE -> storeScriptVariable(name, value, vimContext)
      Scope.WINDOW_VARIABLE -> storeWindowVariable(name, value, editor)
      Scope.TABPAGE_VARIABLE -> storeTabVariable(name, value, editor)
      Scope.FUNCTION_VARIABLE -> storeFunctionVariable(name, value, vimContext)
      Scope.LOCAL_VARIABLE -> storeLocalVariable(name, value, vimContext)
      Scope.BUFFER_VARIABLE -> storeBufferVariable(name, value, editor)
      Scope.VIM_VARIABLE -> storeVimVariable(name, value, editor, context, vimContext)
    }
  }

  override fun getNullableVariableValue(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType? {
    val scope = variable.scope ?: getDefaultVariableScope(vimContext)
    val name = variable.name.evaluate(editor, context, vimContext).value
    return when (scope) {
      Scope.GLOBAL_VARIABLE -> getGlobalVariableValue(name)
      Scope.SCRIPT_VARIABLE -> getScriptVariable(name, vimContext)
      Scope.WINDOW_VARIABLE -> getWindowVariable(name, editor)
      Scope.TABPAGE_VARIABLE -> getTabVariable(name, editor)
      Scope.FUNCTION_VARIABLE -> getFunctionVariable(name, vimContext)
      Scope.LOCAL_VARIABLE -> getLocalVariable(name, vimContext)
      Scope.BUFFER_VARIABLE -> getBufferVariable(name, editor)
      Scope.VIM_VARIABLE -> getVimVariable(name, editor, context, vimContext)
    }
  }

  override fun getNonNullVariableValue(variable: Variable, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    return getNullableVariableValue(variable, editor, context, vimContext)
      ?: throw ExException(
        "E121: Undefined variable: " +
          (if (variable.scope != null) variable.scope.c + ":" else "") +
          variable.name.evaluate(editor, context, vimContext).value
      )
  }

  override fun getGlobalVariableValue(name: String): VimDataType? {
    return globalVariables[name]
  }

  protected open fun getScriptVariable(name: String, vimContext: VimLContext): VimDataType? {
    val script = vimContext.getScript() ?: throw ExException("E121: Undefined variable: s:$name")
    return script.scriptVariables[name]
  }

  protected open fun getWindowVariable(name: String, editor: VimEditor): VimDataType? {
    return getWindowVariables(editor)[name]
  }

  protected open fun getTabVariable(name: String, editor: VimEditor): VimDataType? {
    return getTabVariables(editor)[name]
  }

  protected open fun getFunctionVariable(name: String, vimContext: VimLContext): VimDataType? {
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

  protected open fun getLocalVariable(name: String, vimContext: VimLContext): VimDataType? {
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

  protected open fun getBufferVariable(name: String, editor: VimEditor): VimDataType? {
    return getBufferVariables(editor)[name]
  }

  protected open fun getVimVariable(name: String, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType? {
    throw ExException("The 'v:' scope is not implemented yet :(")
  }

  protected open fun storeGlobalVariable(name: String, value: VimDataType) {
    globalVariables[name] = value
  }

  protected open fun storeScriptVariable(name: String, value: VimDataType, vimContext: VimLContext) {
    val script = vimContext.getScript() ?: throw ExException("E461: Illegal variable name: s:$name")
    script.scriptVariables[name] = value
  }

  protected open fun storeWindowVariable(name: String, value: VimDataType, editor: VimEditor) {
    getWindowVariables(editor)[name] = value
  }

  protected open fun storeTabVariable(name: String, value: VimDataType, editor: VimEditor) {
    getTabVariables(editor)[name] = value
  }

  protected open fun storeFunctionVariable(name: String, value: VimDataType, vimContext: VimLContext) {
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

  protected open fun storeLocalVariable(name: String, value: VimDataType, vimContext: VimLContext) {
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

  protected open fun storeBufferVariable(name: String, value: VimDataType, editor: VimEditor) {
    getBufferVariables(editor)[name] = value
  }

  protected open fun storeVimVariable(name: String, value: VimDataType, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext) {
    throw ExException("The 'v:' scope is not implemented yet :(")
  }

  override fun clear() {
    globalVariables.clear()
  }
}
