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
import com.intellij.vim.api.scopes.ListenersScope
import com.intellij.vim.api.scopes.MappingScope
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.impl.state.toMappingMode
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.selectionType
import com.maddyhome.idea.vim.vimscript.model.VimPluginContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Scope
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import com.maddyhome.idea.vim.vimscript.services.VariableService
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

open class VimScopeImpl(
  // todo: passing ListenerOwner.IdeaVim.System is temporary
  private val listenerOwner: ListenerOwner = ListenerOwner.IdeaVim.System,
) : VimScope {
  override var mode: Mode
    get() {
      return injector.vimState.mode.toMappingMode().toMode()
    }
    set(value) {
      // a lot of custom logic
    }
  private val vimEditor: VimEditor
    get() = injector.editorService.getFocusedEditor()!!

  override fun getSelectionTypeForCurrentMode(): TextSelectionType? {
    val typeInEditor = injector.vimState.mode.selectionType ?: return null
    return typeInEditor.toTextSelectionType()
  }

  override fun <T : Any> getVariable(name: String, type: KType): T {
    val (name, scope) = parseVariableName(name)
    val variableService: VariableService = injector.variableService
    val variable = Variable(scope, name)
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val variableValue: VimDataType? =
      variableService.getNullableVariableValue(variable, vimEditor, context, VimPluginContext)
    if (variableValue == null) {
      throw IllegalArgumentException("Variable with name $name does not exist")
    }
    val value: T = parseVariableValue(variableValue, type)
    return value
  }

  private fun <T : Any> parseVariableValue(vimDataType: VimDataType, type: KType): T {
    val clazz: KClass<*> = type.classifier as KClass<*>
    return when (clazz) {
      Int::class -> {
        if (vimDataType is VimInt) {
          vimDataType.value
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
            list.add(parseVariableValue(value, listArgumentType) ?: continue)
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
            val keyValue: String = parseVariableValue(key, keyArgumentType)
            val valueValue: Any = parseVariableValue(value, valueArgumentType)

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
        return VimScopeImpl().function()
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

  // todo: Probably we don't need it
  override fun exitVisualMode() {
    vimEditor.exitVisualMode()
  }

  override fun editor(block: EditorScope.() -> Unit) {
    val editorScope = EditorScopeImpl()
    editorScope.block()
  }

  override fun mappings(block: MappingScope.() -> Unit) {
    val mappingScope = MappingScopeImpl()
    mappingScope.block()
  }

  override fun listeners(block: ListenersScope.() -> Unit) {
    val listenersScope = ListenerScopeImpl(listenerOwner)
    listenersScope.block()
  }
}