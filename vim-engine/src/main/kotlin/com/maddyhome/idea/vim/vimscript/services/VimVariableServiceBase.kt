/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.Key
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getOrPutBufferData
import com.maddyhome.idea.vim.api.getOrPutTabData
import com.maddyhome.idea.vim.api.getOrPutWindowData
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.ExecutableContext
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionDeclaration
import com.maddyhome.idea.vim.vimscript.model.statements.FunctionFlag
import com.maddyhome.idea.vim.vimscript.model.variables.HighLightVariable
import com.maddyhome.idea.vim.vimscript.model.variables.RegisterVariable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

abstract class VimVariableServiceBase : VariableService {
  private var globalVariables: MutableMap<String, VimDataType> = mutableMapOf()
  private val windowVariablesKey = Key<MutableMap<String, VimDataType>>("TabVariables")
  private val bufferVariablesKey = Key<MutableMap<String, VimDataType>>("BufferVariables")
  private val tabVariablesKey = Key<MutableMap<String, VimDataType>>("WindowVariables")
  protected var vimVariables: MutableMap<String, VimDataType> = mutableMapOf()

  private fun getWindowVariables(editor: VimEditor) =
    injector.vimStorageService.getOrPutWindowData(editor, windowVariablesKey) { mutableMapOf() }

  private fun getBufferVariables(editor: VimEditor) =
    injector.vimStorageService.getOrPutBufferData(editor, bufferVariablesKey) { mutableMapOf() }

  private fun getTabVariables(editor: VimEditor) =
    injector.vimStorageService.getOrPutTabData(editor, tabVariablesKey) { mutableMapOf() }

  protected fun getDefaultVariableScope(executable: VimLContext): Scope {
    return when (executable.getExecutableContext(executable)) {
      ExecutableContext.SCRIPT, ExecutableContext.COMMAND_LINE -> Scope.GLOBAL_VARIABLE
      ExecutableContext.FUNCTION -> Scope.LOCAL_VARIABLE
    }
  }

  override fun isVariableLocked(
    variable: Variable,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): Boolean {
    return getNullableVariableValue(variable, editor, context, vimContext)?.isLocked ?: false
  }

  override fun lockVariable(
    variable: Variable,
    depth: Int,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ) {
    val value = getNullableVariableValue(variable, editor, context, vimContext) ?: return
    value.lockOwner = variable
    value.lockVar(depth)
  }

  override fun unlockVariable(
    variable: Variable,
    depth: Int,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ) {
    val value = getNullableVariableValue(variable, editor, context, vimContext) ?: return
    value.unlockVar(depth)
  }

  override fun getGlobalVariables(): Map<String, VimDataType> {
    return globalVariables
  }

  override fun storeVariable(
    variable: Variable,
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ) {
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

  private fun extractVariableName(variable: Variable): String {
    val parts = variable.name.parts
    if (parts.size == 1 && parts[0] is SimpleExpression) {
      val simpleExpression = parts[0] as SimpleExpression
      val data = simpleExpression.data
      if (data is VimString) {
        return data.value
      }
    }

    throw exExceptionMessage("variable.extract.name.error")
  }

  override fun getNullableVariableValue(
    variable: Variable,
    editor: VimEditor?,
    context: ExecutionContext?,
    vimContext: VimLContext?,
  ): VimDataType? {
    val scope = variable.scope
      ?: if (vimContext != null) {
        getDefaultVariableScope(vimContext)
      } else {
        throw exExceptionMessage("variable.scope.vimcontext.required")
      }

    val name = if (editor != null && context != null && vimContext != null) {
      variable.name.evaluate(editor, context, vimContext).value
    } else {
      extractVariableName(variable)
    }

    return when (scope) {
      Scope.GLOBAL_VARIABLE -> getGlobalVariableValue(name)
      Scope.SCRIPT_VARIABLE -> {
        if (vimContext == null) throw exExceptionMessage("variable.script.vimcontext.required")
        getScriptVariable(name, vimContext)
      }
      Scope.WINDOW_VARIABLE -> {
        if (editor == null) throw exExceptionMessage("variable.window.editor.required")
        getWindowVariable(name, editor)
      }
      Scope.TABPAGE_VARIABLE -> {
        if (editor == null) throw exExceptionMessage("variable.tabpage.editor.required")
        getTabVariable(name, editor)
      }
      Scope.FUNCTION_VARIABLE -> {
        if (vimContext == null) throw exExceptionMessage("variable.function.vimcontext.required")
        getFunctionVariable(name, vimContext)
      }
      Scope.LOCAL_VARIABLE -> {
        if (vimContext == null) throw exExceptionMessage("variable.local.vimcontext.required")
        getLocalVariable(name, vimContext)
      }
      Scope.BUFFER_VARIABLE -> {
        if (editor == null) throw exExceptionMessage("variable.buffer.editor.required")
        getBufferVariable(name, editor)
      }
      Scope.VIM_VARIABLE -> {
        getVimVariable(name, editor, context, vimContext)
      }
    }
  }

  override fun getNonNullVariableValue(
    variable: Variable,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    return getNullableVariableValue(variable, editor, context, vimContext)
      ?: throw exExceptionMessage(
        "E121",
        (if (variable.scope != null) variable.scope.c + ":" else "") +
          variable.name.evaluate(editor, context, vimContext).value,
      )
  }

  override fun getGlobalVariableValue(name: String): VimDataType? {
    return globalVariables[name]
  }

  protected open fun getScriptVariable(name: String, vimContext: VimLContext): VimDataType? {
    val script = vimContext.getScript() ?: throw exExceptionMessage("E121", "s:$name")
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

  @Suppress("SpellCheckingInspection")
  protected open fun getVimVariable(
    name: String,
    editor: VimEditor?,
    context: ExecutionContext?,
    vimContext: VimLContext?,
  ): VimDataType? {
    // Note that the v:count variables might be incorrect in scenarios other than mappings, when there is a command in
    // progress. However, I've only seen it used inside mappings, so don't know
    return when (name) {
      "count" -> VimInt(KeyHandler.getInstance().keyHandlerState.commandBuilder.calculateCount0Snapshot())
      "count1" -> VimInt(
        KeyHandler.getInstance().keyHandlerState.commandBuilder.calculateCount0Snapshot().coerceAtLeast(1)
      )

      "searchforward" -> VimInt(if (injector.searchGroup.getLastSearchDirection() == Direction.FORWARDS) 1 else 0)
      "hlsearch" -> {
        if (editor == null) throw exExceptionMessage("variable.vim.editor.required")
        if (context == null) throw exExceptionMessage("variable.vim.context.required")
        if (vimContext == null) throw exExceptionMessage("variable.vim.vimcontext.required")
        HighLightVariable().evaluate(name, editor, context, vimContext)
      }

      "register" -> {
        if (editor == null) throw exExceptionMessage("variable.vim.editor.required")
        if (context == null) throw exExceptionMessage("variable.vim.context.required")
        if (vimContext == null) throw exExceptionMessage("variable.vim.vimcontext.required")
        RegisterVariable().evaluate(name, editor, context, vimContext)
      }

      else -> throw exExceptionMessage("variable.vim.not.implemented", name)
    }
  }

  override fun storeGlobalVariable(name: String, value: VimDataType) {
    globalVariables[name] = value
  }

  protected open fun storeScriptVariable(name: String, value: VimDataType, vimContext: VimLContext) {
    val script = vimContext.getScript() ?: throw exExceptionMessage("E461", "s:$name")
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
      throw exExceptionMessage("E461", "a:$name")
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
      throw exExceptionMessage("E461", "l:$name")
    }
  }

  protected open fun storeBufferVariable(name: String, value: VimDataType, editor: VimEditor) {
    getBufferVariables(editor)[name] = value
  }

  protected open fun storeVimVariable(
    name: String,
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ) {
    throw exExceptionMessage("variable.scope.vim.not.implemented")
  }

  override fun <T : Any> convertToKotlinType(
    vimDataType: VimDataType,
    type: KType,
  ): T {
    val clazz: KClass<*> = type.classifier as KClass<*>
    return when (clazz) {
      Int::class -> {
        if (vimDataType is VimInt) {
          vimDataType.value
        } else if(vimDataType is VimString && vimDataType.value.toIntOrNull() != null) {
          vimDataType.value.toInt()
        } else {
          throw IllegalArgumentException("Expected Int, but got ${vimDataType::class.simpleName}")
        }
      }

      String::class -> {
        if (vimDataType is VimString) {
          vimDataType.value
        } else {
          throw IllegalArgumentException("Expected String, but got ${vimDataType::class.simpleName}")
        }
      }

      Double::class -> {
        if (vimDataType is VimFloat) {
          vimDataType.value
        } else {
          throw IllegalArgumentException("Expected Double, but got ${vimDataType::class.simpleName}")
        }
      }

      List::class -> {
        if (vimDataType is VimList) {
          val list = mutableListOf<Any>()
          val values = vimDataType.values
          val listArgumentType: KType = type.arguments.firstNotNullOf { it.type }
          for (value in values) {
            list.add(convertToKotlinType(value, listArgumentType))
          }
          list.toList()
        } else {
          throw IllegalArgumentException("Expected List, but got ${vimDataType::class.simpleName}")
        }
      }

      Map::class -> {
        if (vimDataType is VimDictionary) {
          val mapArgumentTypes: List<KType> = type.arguments.mapNotNull { it.type }
          val values: HashMap<VimString, VimDataType> = vimDataType.dictionary

          // the fist argument has to be string
          val keyArgumentType: KType = mapArgumentTypes[0]
          if (keyArgumentType != String::class.createType()) {
            throw IllegalArgumentException("Expected Map with String as key, but got ${vimDataType::class.simpleName}")
          }

          val valueArgumentType: KType = mapArgumentTypes[1]

          val map: MutableMap<String, Any> = mutableMapOf()
          for ((key, value) in values) {
            val keyValue: String = convertToKotlinType(key, keyArgumentType)
            val valueValue: Any = convertToKotlinType(value, valueArgumentType)

            map[keyValue] = valueValue
          }
          map.toMap()
        } else {
          throw IllegalArgumentException("Expected Map, but got ${vimDataType::class.simpleName}")
        }
      }

      else -> throw IllegalArgumentException("Unsupported type: ${clazz.simpleName}")
    } as T
  }

  override fun clear() {
    globalVariables.clear()
  }

  override fun getVimVariable(name: String): VimDataType? {
    return vimVariables[name]
  }

  override fun storeVimVariable(name: String, value: VimDataType) {
    vimVariables[name] = value
  }

  override fun convertToVimDataType(value: Any, type: KType): VimDataType {
    val clazz: KClass<*> = type.classifier as KClass<*>
    return when (clazz) {
      Int::class -> VimInt(value as Int)
      String::class -> VimString(value as String)
      Boolean::class -> if (value as Boolean) VimInt.ONE else VimInt.ZERO
      Double::class -> VimFloat(value as Double)

      List::class -> {
        val list = value as List<*>
        val listArgumentType: KType = type.arguments.firstNotNullOf { it.type }
        val vimValues = mutableListOf<VimDataType>()

        for (item in list) {
          if (item != null) {
            vimValues.add(convertToVimDataType(item, listArgumentType))
          } else {
            throw IllegalArgumentException("List cannot contain null values")
          }
        }

        VimList(vimValues)
      }

      Map::class -> {
        val map = value as Map<*, *>
        val mapArgumentTypes: List<KType> = type.arguments.mapNotNull { it.type }

        // the first argument has to be string
        val keyArgumentType: KType = mapArgumentTypes[0]
        if (keyArgumentType != String::class.createType()) {
          throw IllegalArgumentException("Map must have String keys for conversion to VimDictionary")
        }

        val valueArgumentType: KType = mapArgumentTypes[1]
        val dictionary = LinkedHashMap<VimString, VimDataType>()

        for ((key, mapValue) in map) {
          if (key != null && mapValue != null) {
            val vimKey = VimString(key as String)
            val vimValue = convertToVimDataType(mapValue, valueArgumentType)
            dictionary[vimKey] = vimValue
          } else {
            throw IllegalArgumentException("Map cannot contain null keys or values")
          }
        }

        VimDictionary(dictionary)
      }

      else -> {
        value as? VimDataType ?: throw IllegalArgumentException("Unsupported type: ${clazz.simpleName}")
      }
    }
  }
}
