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

interface VimPluginScope {
  val editor: VimEditor
  val context: ExecutionContext
  val vimPluginApi: VimPluginApi
}

val VimPluginScope.mode: Mode
  get() = vimPluginApi.getMode(this)

fun VimPluginScope.getSelectionTypeForCurrentMode(): TextSelectionType? {
  return vimPluginApi.getSelectionTypeForCurrentMode(this)
}

fun VimPluginScope.getVimVariableInt(name: String, vimVariableScope: VimVariablesScope): Int? {
  return vimPluginApi.getVimVariableInt(this, vimVariableScope, name)
}

fun VimInitPluginScope.exportOperatorFunction(name: String, function: VimPluginScope.() -> Boolean) {
  vimPluginApi.exportOperatorFunction(name, this, function)
}

fun VimPluginScope.setOperatorFunction(name: String) {
  vimPluginApi.setOperatorFunction(this, name)
}

fun VimPluginScope.normal(command: String) {
  vimPluginApi.executeNormal(this, command)
}

fun VimPluginScope.exitVisualMode() {
  vimPluginApi.exitVisualMode(this)
}