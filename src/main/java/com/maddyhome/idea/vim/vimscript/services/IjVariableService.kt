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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jdom.Element

@State(name = "VimVariables", storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)])
internal class IjVariableService : VimVariableServiceBase(), PersistentStateComponent<Element?> {
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
          vimVariables[variableElement.getAttributeValue("key")] = VimInt(variableElement.getAttributeValue("value"))
        }
      }
    }
  }
}
