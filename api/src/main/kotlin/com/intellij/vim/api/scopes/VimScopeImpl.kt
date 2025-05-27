/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.Mode
import com.intellij.vim.api.TextSelectionType
import com.intellij.vim.api.VimPluginApi
import com.intellij.vim.api.VimVariablesScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor

internal class VimScopeImpl(
   val editor: VimEditor,
   val context: ExecutionContext,
   val vimPluginApi: VimPluginApi,
) : VimScope {
  override val mode: Mode
    get() = vimPluginApi.getMode(editor)

  override fun getSelectionTypeForCurrentMode(): TextSelectionType? {
    return vimPluginApi.getSelectionTypeForCurrentMode(editor)
  }

  override fun getVimVariableInt(
    name: String,
    vimVariableScope: VimVariablesScope,
  ): Int? {
    return vimPluginApi.getVimVariableInt(vimVariableScope, name)
  }

  override fun exportOperatorFunction(
    name: String,
    function: VimScope.() -> Boolean,
  ) {
    vimPluginApi.exportOperatorFunction(name, vimPluginApi, function)
  }

  override fun setOperatorFunction(name: String) {
    vimPluginApi.setOperatorFunction(name)
  }

  override fun normal(command: String) {
    vimPluginApi.executeNormal(editor, command)
  }

  override fun exitVisualMode() {
    vimPluginApi.exitVisualMode(editor)
  }

  override fun nmap(fromKeys: String, toKeys: String) {
    addMapping(fromKeys, toKeys, isRecursive = true, Mode.NORMAL)
  }

  override fun vmap(fromKeys: String, toKeys: String) {
    addMapping(fromKeys, toKeys, isRecursive = true, Mode.VISUAL)
  }

  override fun nmap(
    fromKeys: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.NORMAL)
  }

  override fun vmap(
    fromKeys: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.VISUAL)
  }

  private fun addMapping(fromKeys: String, toKeys: String, isRecursive: Boolean, vararg modes: Mode) {
    vimPluginApi.addMapping(fromKeys, toKeys, isRecursive, *modes)
  }

  private fun addMapping(
    fromKeys: String,
    isRecursive: Boolean,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
    vararg modes: Mode,
  ) {
    vimPluginApi.addMapping(vimPluginApi, fromKeys, isRecursive, isRepeatable, action, *modes)
  }

  private fun removeMapping(fromKeys: String, vararg modes: Mode) {
    vimPluginApi.removeMapping(fromKeys, *modes)
  }
}