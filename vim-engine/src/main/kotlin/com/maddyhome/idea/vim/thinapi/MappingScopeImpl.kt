/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.scopes.MappingScope
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.key.MappingOwner

class MappingScopeImpl(
  private val mappingOwner: MappingOwner
): MappingScope {
  override fun nmap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.NORMAL)
  }

  override fun vmap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.VISUAL)
  }

  override fun nmap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.NORMAL)
  }

  override fun vmap(
    from: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.VISUAL)
  }

  override fun nmap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.NORMAL)
    addMapping(keys, label, true, MappingMode.NORMAL)
  }

  override fun vmap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.VISUAL)
    addMapping(keys, label, true, MappingMode.VISUAL)
  }

  private fun addMapping(
    from: String,
    to: String,
    isRecursive: Boolean,
    vararg mode: MappingMode,
  ) {
    injector.keyGroup.putKeyMapping(
      modes = mode.toSet(),
      fromKeys = injector.parser.parseKeys(from),
      toKeys = injector.parser.parseKeys(to),
      recursive = isRecursive,
      owner = mappingOwner
    )
  }

  private fun addMapping(
    from: String,
    isRecursive: Boolean,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
    vararg mode: MappingMode,
  ) {
    val extensionHandler: ExtensionHandler = object : ExtensionHandler {
      override val isRepeatable: Boolean
        get() = isRepeatable

      override fun execute(
        editor: VimEditor,
        context: ExecutionContext,
        operatorArguments: OperatorArguments,
      ) {
        return VimScopeImpl().action()
      }
    }

    injector.keyGroup.putKeyMapping(
      modes = mode.toSet(),
      fromKeys = injector.parser.parseKeys(from),
      owner = mappingOwner,
      recursive = isRecursive,
      extensionHandler = extensionHandler
    )
  }
}