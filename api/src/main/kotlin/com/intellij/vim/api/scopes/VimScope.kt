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
import com.intellij.vim.api.VimVariablesScope

interface VimScope {
  val mode: Mode
  fun getSelectionTypeForCurrentMode(): TextSelectionType?
  fun getVimVariableInt(name: String, vimVariableScope: VimVariablesScope): Int?
  fun exportOperatorFunction(name: String, function: VimScope.() -> Boolean)
  fun setOperatorFunction(name: String)
  fun normal(command: String)
  fun exitVisualMode()
  fun nmap(fromKeys: String, toKeys: String)
  fun vmap(fromKeys: String, toKeys: String)
  fun nmap(fromKeys: String, isRepeatable: Boolean, action: VimScope.() -> Unit)
  fun vmap(fromKeys: String, isRepeatable: Boolean, action: VimScope.() -> Unit)
}