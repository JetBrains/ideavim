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

@VimPluginDsl
interface VimScope {
  var mode: Mode
  fun getSelectionTypeForCurrentMode(): TextSelectionType?
  fun getVariableInt(name: String): Int?
  fun exportOperatorFunction(name: String, function: VimScope.() -> Boolean)
  fun setOperatorFunction(name: String)
  fun normal(command: String)

  // todo: Use mode instead
  fun exitVisualMode()

  fun editor(block: EditorScope.() -> Unit)
  fun mappings(block: MappingScope.() -> Unit)
}