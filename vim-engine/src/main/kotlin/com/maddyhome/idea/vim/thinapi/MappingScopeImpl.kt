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
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.key.MappingOwner

class MappingScopeImpl(
  private val listenerOwner: ListenerOwner,
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

  override fun map(from: String, to: String) {
    addMapping(from, to, isRecursive = true, *MappingMode.ALL.toTypedArray())
  }

  override fun map(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, *MappingMode.ALL.toTypedArray())
  }

  override fun map(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, *MappingMode.ALL.toTypedArray())
    addMapping(keys, label, true, *MappingMode.ALL.toTypedArray())
  }

  override fun xmap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.VISUAL)
  }

  override fun xmap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.VISUAL)
  }

  override fun xmap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.VISUAL)
    addMapping(keys, label, true, MappingMode.VISUAL)
  }

  override fun smap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.SELECT)
  }

  override fun smap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.SELECT)
  }

  override fun smap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.SELECT)
    addMapping(keys, label, true, MappingMode.SELECT)
  }

  override fun omap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.OP_PENDING)
  }

  override fun omap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.OP_PENDING)
  }

  override fun omap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.OP_PENDING)
    addMapping(keys, label, true, MappingMode.OP_PENDING)
  }

  override fun imap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.INSERT)
  }

  override fun imap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.INSERT)
  }

  override fun imap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.INSERT)
    addMapping(keys, label, true, MappingMode.INSERT)
  }

  override fun cmap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.CMD_LINE)
  }

  override fun cmap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.CMD_LINE)
  }

  override fun cmap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.CMD_LINE)
    addMapping(keys, label, true, MappingMode.CMD_LINE)
  }

  override fun nnoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.NORMAL)
  }

  override fun nnoremap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.NORMAL)
  }

  override fun nnoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.NORMAL)
    addMapping(keys, label, false, MappingMode.NORMAL)
  }

  override fun vnoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.VISUAL)
  }

  override fun vnoremap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.VISUAL)
  }

  override fun vnoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.VISUAL)
    addMapping(keys, label, false, MappingMode.VISUAL)
  }

  override fun noremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, *MappingMode.ALL.toTypedArray())
  }

  override fun noremap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, *MappingMode.ALL.toTypedArray())
  }

  override fun noremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, *MappingMode.ALL.toTypedArray())
    addMapping(keys, label, false, *MappingMode.ALL.toTypedArray())
  }

  override fun xnoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.VISUAL)
  }

  override fun xnoremap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.VISUAL)
  }

  override fun xnoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.VISUAL)
    addMapping(keys, label, false, MappingMode.VISUAL)
  }

  override fun snoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.SELECT)
  }

  override fun snoremap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.SELECT)
  }

  override fun snoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.SELECT)
    addMapping(keys, label, false, MappingMode.SELECT)
  }

  override fun onoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.OP_PENDING)
  }

  override fun onoremap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.OP_PENDING)
  }

  override fun onoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.OP_PENDING)
    addMapping(keys, label, false, MappingMode.OP_PENDING)
  }

  override fun inoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.INSERT)
  }

  override fun inoremap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.INSERT)
  }

  override fun inoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.INSERT)
    addMapping(keys, label, false, MappingMode.INSERT)
  }

  override fun cnoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.CMD_LINE)
  }

  override fun cnoremap(from: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.CMD_LINE)
  }

  override fun cnoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.CMD_LINE)
    addMapping(keys, label, false, MappingMode.CMD_LINE)
  }

  override fun nunmap(keys: String) {
    removeMapping(keys, MappingMode.NORMAL)
  }

  override fun vunmap(keys: String) {
    removeMapping(keys, MappingMode.VISUAL)
  }

  override fun unmap(keys: String) {
    removeMapping(keys, *MappingMode.ALL.toTypedArray())
  }

  override fun xunmap(keys: String) {
    removeMapping(keys, MappingMode.VISUAL)
  }

  override fun sunmap(keys: String) {
    removeMapping(keys, MappingMode.SELECT)
  }

  override fun ounmap(keys: String) {
    removeMapping(keys, MappingMode.OP_PENDING)
  }

  override fun iunmap(keys: String) {
    removeMapping(keys, MappingMode.INSERT)
  }

  override fun cunmap(keys: String) {
    removeMapping(keys, MappingMode.CMD_LINE)
  }

  private fun removeMapping(keys: String, vararg mode: MappingMode) {
    injector.keyGroup.removeKeyMapping(
      modes = mode.toSet(),
      keys = injector.parser.parseKeys(keys)
    )
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
        return VimScopeImpl(listenerOwner, mappingOwner).action()
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
