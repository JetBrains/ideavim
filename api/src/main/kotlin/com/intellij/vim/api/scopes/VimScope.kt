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

interface VimScope {
  val editor: VimEditor
  val context: ExecutionContext
  val vimPluginApi: VimPluginApi
}

val VimScope.mode: Mode
  get() = vimPluginApi.getMode(this)

fun VimScope.getSelectionTypeForCurrentMode(): TextSelectionType? {
  return vimPluginApi.getSelectionTypeForCurrentMode(this)
}

fun VimScope.getVimVariableInt(name: String, vimVariableScope: VimVariablesScope): Int? {
  return vimPluginApi.getVimVariableInt(this, vimVariableScope, name)
}

fun VimInitPluginScope.exportOperatorFunction(name: String, function: VimScope.() -> Boolean) {
  vimPluginApi.exportOperatorFunction(name, this, function)
}

fun VimScope.setOperatorFunction(name: String) {
  vimPluginApi.setOperatorFunction(this, name)
}

fun VimScope.normal(command: String) {
  vimPluginApi.executeNormal(this, command)
}

fun VimScope.exitVisualMode() {
  vimPluginApi.exitVisualMode(this)
}