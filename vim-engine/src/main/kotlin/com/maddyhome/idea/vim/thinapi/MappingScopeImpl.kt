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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MappingScopeImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner
): MappingScope {
  private val coroutineScope = CoroutineScope(Dispatchers.Unconfined)

  override suspend fun nmap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.NORMAL)
  }

  override suspend fun vmap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.VISUAL)
  }

  override suspend fun nmap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.NORMAL)
  }

  override suspend fun vmap(
    from: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.VISUAL)
  }

  override suspend fun nmap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.NORMAL)
    addMapping(keys, label, true, MappingMode.NORMAL)
  }

  override suspend fun vmap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.VISUAL)
    addMapping(keys, label, true, MappingMode.VISUAL)
  }

  override suspend fun map(from: String, to: String) {
    addMapping(from, to, isRecursive = true, *MappingMode.ALL.toTypedArray())
  }

  override suspend fun map(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, *MappingMode.ALL.toTypedArray())
  }

  override suspend fun map(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, *MappingMode.ALL.toTypedArray())
    addMapping(keys, label, true, *MappingMode.ALL.toTypedArray())
  }

  override suspend fun xmap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.VISUAL)
  }

  override suspend fun xmap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.VISUAL)
  }

  override suspend fun xmap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.VISUAL)
    addMapping(keys, label, true, MappingMode.VISUAL)
  }

  override suspend fun smap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.SELECT)
  }

  override suspend fun smap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.SELECT)
  }

  override suspend fun smap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.SELECT)
    addMapping(keys, label, true, MappingMode.SELECT)
  }

  override suspend fun omap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.OP_PENDING)
  }

  override suspend fun omap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.OP_PENDING)
  }

  override suspend fun omap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.OP_PENDING)
    addMapping(keys, label, true, MappingMode.OP_PENDING)
  }

  override suspend fun imap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.INSERT)
  }

  override suspend fun imap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.INSERT)
  }

  override suspend fun imap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.INSERT)
    addMapping(keys, label, true, MappingMode.INSERT)
  }

  override suspend fun cmap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.CMD_LINE)
  }

  override suspend fun cmap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = true, isRepeatable, action, MappingMode.CMD_LINE)
  }

  override suspend fun cmap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, true, isRepeatable, action, MappingMode.CMD_LINE)
    addMapping(keys, label, true, MappingMode.CMD_LINE)
  }

  override suspend fun nnoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.NORMAL)
  }

  override suspend fun nnoremap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.NORMAL)
  }

  override suspend fun nnoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.NORMAL)
    addMapping(keys, label, false, MappingMode.NORMAL)
  }

  override suspend fun vnoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.VISUAL)
  }

  override suspend fun vnoremap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.VISUAL)
  }

  override suspend fun vnoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.VISUAL)
    addMapping(keys, label, false, MappingMode.VISUAL)
  }

  override suspend fun noremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, *MappingMode.ALL.toTypedArray())
  }

  override suspend fun noremap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, *MappingMode.ALL.toTypedArray())
  }

  override suspend fun noremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, *MappingMode.ALL.toTypedArray())
    addMapping(keys, label, false, *MappingMode.ALL.toTypedArray())
  }

  override suspend fun xnoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.VISUAL)
  }

  override suspend fun xnoremap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.VISUAL)
  }

  override suspend fun xnoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.VISUAL)
    addMapping(keys, label, false, MappingMode.VISUAL)
  }

  override suspend fun snoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.SELECT)
  }

  override suspend fun snoremap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.SELECT)
  }

  override suspend fun snoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.SELECT)
    addMapping(keys, label, false, MappingMode.SELECT)
  }

  override suspend fun onoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.OP_PENDING)
  }

  override suspend fun onoremap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.OP_PENDING)
  }

  override suspend fun onoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.OP_PENDING)
    addMapping(keys, label, false, MappingMode.OP_PENDING)
  }

  override suspend fun inoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.INSERT)
  }

  override suspend fun inoremap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.INSERT)
  }

  override suspend fun inoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.INSERT)
    addMapping(keys, label, false, MappingMode.INSERT)
  }

  override suspend fun cnoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.CMD_LINE)
  }

  override suspend fun cnoremap(from: String, isRepeatable: Boolean, action: suspend VimScope.() -> Unit) {
    addMapping(from, isRecursive = false, isRepeatable, action, MappingMode.CMD_LINE)
  }

  override suspend fun cnoremap(
    keys: String,
    label: String,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
  ) {
    addMapping(label, false, isRepeatable, action, MappingMode.CMD_LINE)
    addMapping(keys, label, false, MappingMode.CMD_LINE)
  }

  override suspend fun nunmap(keys: String) {
    removeMapping(keys, MappingMode.NORMAL)
  }

  override suspend fun vunmap(keys: String) {
    removeMapping(keys, MappingMode.VISUAL)
  }

  override suspend fun unmap(keys: String) {
    removeMapping(keys, *MappingMode.ALL.toTypedArray())
  }

  override suspend fun xunmap(keys: String) {
    removeMapping(keys, MappingMode.VISUAL)
  }

  override suspend fun sunmap(keys: String) {
    removeMapping(keys, MappingMode.SELECT)
  }

  override suspend fun ounmap(keys: String) {
    removeMapping(keys, MappingMode.OP_PENDING)
  }

  override suspend fun iunmap(keys: String) {
    removeMapping(keys, MappingMode.INSERT)
  }

  override suspend fun cunmap(keys: String) {
    removeMapping(keys, MappingMode.CMD_LINE)
  }

  private suspend fun removeMapping(keys: String, vararg mode: MappingMode) {
    injector.keyGroup.removeKeyMapping(
      modes = mode.toSet(),
      keys = injector.parser.parseKeys(keys)
    )
  }

  private suspend fun addMapping(
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

  private suspend fun addMapping(
    from: String,
    isRecursive: Boolean,
    isRepeatable: Boolean,
    action: suspend VimScope.() -> Unit,
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
        // todo: see if previously launched job has finished
        coroutineScope.launch {  VimScopeImpl(listenerOwner, mappingOwner).action() }
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
