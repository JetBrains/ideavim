/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment
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
import com.maddyhome.idea.vim.vimscript.model.expressions.VariableExpression
import org.jdom.Element

@State(
  name = "VimVariables",
  storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)]
)
internal class IjVariableService : VimVariableServiceBase(), PersistentStateComponent<Element?> {
  override fun storeVariable(
    variable: VariableExpression,
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ) {
    super.storeVariable(variable, value, editor, context, vimContext)

    val scope = variable.scope ?: getDefaultVariableScope(vimContext)
    if (scope == Scope.GLOBAL_VARIABLE) {
      val scopeForGlobalEnvironment = variable.scope?.toString() ?: ""
      VimScriptGlobalEnvironment.getInstance()
        .variables[scopeForGlobalEnvironment + variable.name.evaluate(editor, context, vimContext).value] = value.simplify()
    }
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

  override fun getState(): Element {
    val element = Element("variables")
    saveData(element)
    return element
  }

  override fun loadState(state: Element) {
    readData(state)
  }

  private fun saveData(element: Element) {
    val vimVariablesElement = Element("vim-variables")
    for ((key, value) in vimVariables.entries) {
      if (value is VimString) {
        val variableElement = Element("variable")
        variableElement.setAttribute("key", key)
        variableElement.setAttribute("value", value.value)
        variableElement.setAttribute("type", "string")
        vimVariablesElement.addContent(variableElement)
      } else if (value is VimInt) {
        val variableElement = Element("variable")
        variableElement.setAttribute("key", key)
        variableElement.setAttribute("value", value.value.toString())
        variableElement.setAttribute("type", "int")
        vimVariablesElement.addContent(variableElement)
      }
    }
    element.addContent(vimVariablesElement)
  }

  private fun readData(element: Element) {
    val vimVariablesElement = element.getChild("vim-variables")
    val variableElements = vimVariablesElement.getChildren("variable")
    for (variableElement in variableElements) {
      when (variableElement.getAttributeValue("type")) {
        "string" -> {
          vimVariables[variableElement.getAttributeValue("key")] = VimString(variableElement.getAttributeValue("value"))
        }

        "int" -> {
          vimVariables[variableElement.getAttributeValue("key")] =
            VimInt.parseNumber(variableElement.getAttributeValue("value")) ?: VimInt.ZERO
        }
      }
    }
  }
}
