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
import com.maddyhome.idea.vim.vimscript.model.expressions.Variable
import org.jdom.Element

@State(name = "VimVariables", storages = [Storage(value = "\$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)])
internal class IjVariableService : VimVariableServiceBase(), PersistentStateComponent<Element?> {
  override fun storeVariable(variable: Variable, value: VimDataType, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext) {
    super.storeVariable(variable, value, editor, context, vimContext)

    val scope = variable.scope ?: getDefaultVariableScope(vimContext)
    if (scope == Scope.GLOBAL_VARIABLE) {
      val scopeForGlobalEnvironment = variable.scope?.toString() ?: ""
      VimScriptGlobalEnvironment.getInstance()
        .variables[scopeForGlobalEnvironment + variable.name.evaluate(editor, context, vimContext)] = value.simplify()
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

  override fun getVimVariable(name: String): VimDataType? {
    val userSetValue = super.getVimVariable(name)
    if (userSetValue != null) return userSetValue

    return when (name) {
      "widget_mode_normal_background_light" -> VimString("#aed586")
      "widget_mode_normal_foreground_light" -> VimString("v:status_bar_fg")
      "widget_mode_insert_background_light" -> VimString("#86aed5")
      "widget_mode_insert_foreground_light" -> VimString("v:status_bar_fg")
      "widget_mode_replace_background_light" -> VimString("#d58686")
      "widget_mode_replace_foreground_light" -> VimString("v:status_bar_fg")
      "widget_mode_command_background_light" -> VimString("#aed586")
      "widget_mode_command_foreground_light" -> VimString("v:status_bar_fg")
      "widget_mode_visual_background_light" -> VimString("#d5aed5")
      "widget_mode_visual_foreground_light" -> VimString("v:status_bar_fg")
//      "widget_mode_visual_line_background_light" -> VimString("")
//      "widget_mode_visual_line_foreground_light" -> VimString("v:status_bar_fg")
//      "widget_mode_visual_block_background_light" -> VimString("")
//      "widget_mode_visual_block_foreground_light" -> VimString("v:status_bar_fg")
      "widget_mode_select_background_light" -> VimString("#d5aed5")
      "widget_mode_select_foreground_light" -> VimString("v:status_bar_fg")
//      "widget_mode_select_line_background_light" -> VimString("")
//      "widget_mode_select_line_foreground_light" -> VimString("v:status_bar_fg")
//      "widget_mode_select_block_background_light" -> VimString("")
//      "widget_mode_select_block_foreground_light" -> VimString("v:status_bar_fg")

      "widget_mode_normal_background_dark" -> VimString("#aed586")
      "widget_mode_normal_foreground_dark" -> VimString("v:status_bar_fg")
      "widget_mode_insert_background_dark" -> VimString("#86aed5")
      "widget_mode_insert_foreground_dark" -> VimString("v:status_bar_fg")
      "widget_mode_replace_background_dark" -> VimString("#d58686")
      "widget_mode_replace_foreground_dark" -> VimString("v:status_bar_fg")
      "widget_mode_command_background_dark" -> VimString("#aed586")
      "widget_mode_command_foreground_dark" -> VimString("v:status_bar_fg")
      "widget_mode_visual_background_dark" -> VimString("#d5aed5")
      "widget_mode_visual_foreground_dark" -> VimString("v:status_bar_fg")
//      "widget_mode_visual_line_background_dark" -> VimString("")
//      "widget_mode_visual_line_foreground_dark" -> VimString("v:status_bar_fg")
//      "widget_mode_visual_block_background_dark" -> VimString("")
//      "widget_mode_visual_block_foreground_dark" -> VimString("v:status_bar_fg")
      "widget_mode_select_background_dark" -> VimString("#d5aed5")
      "widget_mode_select_foreground_dark" -> VimString("v:status_bar_fg")
//      "widget_mode_select_line_background_dark" -> VimString("")
//      "widget_mode_select_line_foreground_dark" -> VimString("v:status_bar_fg")
//      "widget_mode_select_block_background_dark" -> VimString("")
//      "widget_mode_select_block_foreground_dark" -> VimString("v:status_bar_fg")

      else -> null
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
      }
    }
    element.addContent(vimVariablesElement)
  }

  private fun readData(element: Element) {
    val vimVariablesElement = element.getChild("vim-variables")
    val variableElements = vimVariablesElement.getChildren("variable")
    for (variableElement in variableElements) {
      if (variableElement.getAttributeValue("type") != "string") continue
      vimVariables[variableElement.getAttributeValue("key")] = VimString(variableElement.getAttributeValue("value"))
    }
  }
}
