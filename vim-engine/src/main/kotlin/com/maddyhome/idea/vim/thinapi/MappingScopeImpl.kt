/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.scopes.MappingScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.key.MappingOwner
import kotlinx.coroutines.runBlocking

class MappingScopeImpl(
  private val listenerOwner: ListenerOwner,
  private val mappingOwner: MappingOwner,
) : MappingScope {

  // ===== Normal, Visual, Select, and Operator-pending modes (map/noremap/unmap) =====

  override fun map(from: String, to: String) {
    addMapping(from, to, isRecursive = true, *MappingMode.NVO.toTypedArray())
  }

  override fun map(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = true, action, *MappingMode.NVO.toTypedArray())
  }

  override fun noremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, *MappingMode.NVO.toTypedArray())
  }

  override fun noremap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = false, action, *MappingMode.NVO.toTypedArray())
  }

  override fun unmap(keys: String) {
    removeMapping(keys, *MappingMode.NVO.toTypedArray())
  }

  override fun hasmapto(to: String): Boolean {
    val toKeys = injector.parser.parseKeys(to)
    return MappingMode.NVO.any { mode ->
      injector.keyGroup.hasmapto(mode, toKeys)
    }
  }

  // ===== Normal mode (nmap/nnoremap/nunmap) =====

  override fun nmap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.NORMAL)
  }

  override fun nmap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = true, action, MappingMode.NORMAL)
  }

  override fun nnoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.NORMAL)
  }

  override fun nnoremap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = false, action, MappingMode.NORMAL)
  }

  override fun nunmap(keys: String) {
    removeMapping(keys, MappingMode.NORMAL)
  }

  override fun nhasmapto(to: String): Boolean {
    return injector.keyGroup.hasmapto(MappingMode.NORMAL, injector.parser.parseKeys(to))
  }

  // ===== Visual mode (vmap/vnoremap/vunmap) =====

  override fun vmap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.VISUAL)
  }

  override fun vmap(
    from: String,
    action: suspend VimApi.() -> Unit,
  ) {
    addMapping(from, isRecursive = true, action, MappingMode.VISUAL)
  }

  override fun vnoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.VISUAL)
  }

  override fun vnoremap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = false, action, MappingMode.VISUAL)
  }

  override fun vunmap(keys: String) {
    removeMapping(keys, MappingMode.VISUAL)
  }

  override fun vhasmapto(to: String): Boolean {
    return injector.keyGroup.hasmapto(MappingMode.VISUAL, injector.parser.parseKeys(to))
  }

  // ===== Visual exclusive mode (xmap/xnoremap/xunmap) =====

  override fun xmap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.VISUAL)
  }

  override fun xmap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = true, action, MappingMode.VISUAL)
  }

  override fun xnoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.VISUAL)
  }

  override fun xnoremap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = false, action, MappingMode.VISUAL)
  }

  override fun xunmap(keys: String) {
    removeMapping(keys, MappingMode.VISUAL)
  }

  override fun xhasmapto(to: String): Boolean {
    return injector.keyGroup.hasmapto(MappingMode.VISUAL, injector.parser.parseKeys(to))
  }

  // ===== Select mode (smap/snoremap/sunmap) =====

  override fun smap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.SELECT)
  }

  override fun smap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = true, action, MappingMode.SELECT)
  }

  override fun snoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.SELECT)
  }

  override fun snoremap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = false, action, MappingMode.SELECT)
  }

  override fun sunmap(keys: String) {
    removeMapping(keys, MappingMode.SELECT)
  }

  override fun shasmapto(to: String): Boolean {
    return injector.keyGroup.hasmapto(MappingMode.SELECT, injector.parser.parseKeys(to))
  }

  // ===== Operator pending mode (omap/onoremap/ounmap) =====

  override fun omap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.OP_PENDING)
  }

  override fun omap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = true, action, MappingMode.OP_PENDING)
  }

  override fun onoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.OP_PENDING)
  }

  override fun onoremap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = false, action, MappingMode.OP_PENDING)
  }

  override fun ounmap(keys: String) {
    removeMapping(keys, MappingMode.OP_PENDING)
  }

  override fun ohasmapto(to: String): Boolean {
    return injector.keyGroup.hasmapto(MappingMode.OP_PENDING, injector.parser.parseKeys(to))
  }

  // ===== Insert mode (imap/inoremap/iunmap) =====

  override fun imap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.INSERT)
  }

  override fun imap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = true, action, MappingMode.INSERT)
  }

  override fun inoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.INSERT)
  }

  override fun inoremap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = false, action, MappingMode.INSERT)
  }

  override fun iunmap(keys: String) {
    removeMapping(keys, MappingMode.INSERT)
  }

  override fun ihasmapto(to: String): Boolean {
    return injector.keyGroup.hasmapto(MappingMode.INSERT, injector.parser.parseKeys(to))
  }

  // ===== Command line mode (cmap/cnoremap/cunmap) =====

  override fun cmap(from: String, to: String) {
    addMapping(from, to, isRecursive = true, MappingMode.CMD_LINE)
  }

  override fun cmap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = true, action, MappingMode.CMD_LINE)
  }

  override fun cnoremap(from: String, to: String) {
    addMapping(from, to, isRecursive = false, MappingMode.CMD_LINE)
  }

  override fun cnoremap(from: String, action: suspend VimApi.() -> Unit) {
    addMapping(from, isRecursive = false, action, MappingMode.CMD_LINE)
  }

  override fun cunmap(keys: String) {
    removeMapping(keys, MappingMode.CMD_LINE)
  }

  override fun chasmapto(to: String): Boolean {
    return injector.keyGroup.hasmapto(MappingMode.CMD_LINE, injector.parser.parseKeys(to))
  }

  // ===== Private helper methods =====

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
    action: suspend VimApi.() -> Unit,
    vararg mode: MappingMode,
  ) {
    val extensionHandler: ExtensionHandler = object : ExtensionHandler {
      override val isRepeatable: Boolean
        get() = true

      override fun execute(
        editor: VimEditor,
        context: ExecutionContext,
        operatorArguments: OperatorArguments,
      ) {
        // XXX: It's not OK to call runBlocking, but let's keep it to have an API.
        runBlocking { VimApiImpl(listenerOwner, mappingOwner).action() }
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
