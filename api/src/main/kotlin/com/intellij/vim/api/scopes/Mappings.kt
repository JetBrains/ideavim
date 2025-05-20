/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.Mode


fun VimInitPluginScope.map(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = true, Mode.OP_PENDING)
}

fun VimInitPluginScope.nmap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = true, Mode.NORMAL)
}

fun VimInitPluginScope.vmap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = true, Mode.VISUAL)
}

fun VimInitPluginScope.omap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = true, Mode.OP_PENDING)
}

fun VimInitPluginScope.imap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = true, Mode.INSERT)
}

fun VimInitPluginScope.cmap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = true, Mode.COMMAND)
}

fun VimInitPluginScope.nnoremap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = false, Mode.NORMAL)
}

fun VimInitPluginScope.vnoremap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = false, Mode.VISUAL)
}

fun VimInitPluginScope.onoremap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = false, Mode.OP_PENDING)
}

fun VimInitPluginScope.inoremap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = false, Mode.INSERT)
}

fun VimInitPluginScope.cnoremap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = false, Mode.COMMAND)
}

fun VimInitPluginScope.noremap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = false, Mode.OP_PENDING)
}

fun VimInitPluginScope.map(fromKeys: String, isRepeatable: Boolean, action: VimPluginScope.() -> Unit) {
  addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.OP_PENDING)
}

fun VimInitPluginScope.nmap(fromKeys: String, isRepeatable: Boolean, action: VimPluginScope.() -> Unit) {
  addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.NORMAL)
}

fun VimInitPluginScope.vmap(fromKeys: String, isRepeatable: Boolean, action: VimPluginScope.() -> Unit) {
  addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.VISUAL)
}

fun VimInitPluginScope.omap(fromKeys: String, isRepeatable: Boolean, action: VimPluginScope.() -> Unit) {
  addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.OP_PENDING)
}

fun VimInitPluginScope.imap(fromKeys: String, isRepeatable: Boolean, action: VimPluginScope.() -> Unit) {
  addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.INSERT)
}

fun VimInitPluginScope.cmap(fromKeys: String, isRepeatable: Boolean, action: VimPluginScope.() -> Unit) {
  addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.COMMAND)
}

fun VimInitPluginScope.unmap(fromKeys: String) {
  removeMapping(fromKeys, Mode.OP_PENDING)
}

fun VimInitPluginScope.nunmap(fromKeys: String) {
  removeMapping(fromKeys, Mode.NORMAL)
}

fun VimInitPluginScope.vunmap(fromKeys: String) {
  removeMapping(fromKeys, Mode.VISUAL)
}

fun VimInitPluginScope.ounmap(fromKeys: String) {
  removeMapping(fromKeys, Mode.OP_PENDING)
}

fun VimInitPluginScope.iunmap(fromKeys: String) {
  removeMapping(fromKeys, Mode.INSERT)
}

fun VimInitPluginScope.cunmap(fromKeys: String) {
  removeMapping(fromKeys, Mode.COMMAND)
}

private fun VimInitPluginScope.addMapping(fromKeys: String, toKeys: String, isRecursive: Boolean, vararg modes: Mode) {
  vimPluginApi.addMapping(this, fromKeys, toKeys, isRecursive, *modes)
}

private fun VimInitPluginScope.addMapping(
  fromKeys: String,
  isRecursive: Boolean,
  isRepeatable: Boolean,
  action: VimPluginScope.() -> Unit,
  vararg modes: Mode,
) {
  vimPluginApi.addMapping(fromKeys, this, isRecursive, isRepeatable, action, *modes)
}

private fun VimInitPluginScope.removeMapping(fromKeys: String, vararg modes: Mode) {
  vimPluginApi.removeMapping(this, fromKeys, *modes)
}