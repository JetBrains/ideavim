/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.Mode


fun VimScope.map(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = true, Mode.OP_PENDING)
}

fun VimScope.nmap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = true, Mode.NORMAL)
}

fun VimScope.vmap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = true, Mode.VISUAL)
}

fun VimScope.omap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = true, Mode.OP_PENDING)
}

fun VimScope.imap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = true, Mode.INSERT)
}

fun VimScope.cmap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = true, Mode.COMMAND)
}

fun VimScope.nnoremap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = false, Mode.NORMAL)
}

fun VimScope.vnoremap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = false, Mode.VISUAL)
}

fun VimScope.onoremap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = false, Mode.OP_PENDING)
}

fun VimScope.inoremap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = false, Mode.INSERT)
}

fun VimScope.cnoremap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = false, Mode.COMMAND)
}

fun VimScope.noremap(fromKeys: String, toKeys: String) {
  addMapping(fromKeys, toKeys, isRecursive = false, Mode.OP_PENDING)
}

fun VimScope.map(fromKeys: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
  addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.OP_PENDING)
}

fun VimScope.nmap(fromKeys: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
  addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.NORMAL)
}

fun VimScope.vmap(fromKeys: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
  addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.VISUAL)
}

fun VimScope.omap(fromKeys: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
  addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.OP_PENDING)
}

fun VimScope.imap(fromKeys: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
  addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.INSERT)
}

fun VimScope.cmap(fromKeys: String, isRepeatable: Boolean, action: VimScope.() -> Unit) {
  addMapping(fromKeys, isRecursive = true, isRepeatable, action, Mode.COMMAND)
}

fun VimScope.unmap(fromKeys: String) {
  removeMapping(fromKeys, Mode.OP_PENDING)
}

fun VimScope.nunmap(fromKeys: String) {
  removeMapping(fromKeys, Mode.NORMAL)
}

fun VimScope.vunmap(fromKeys: String) {
  removeMapping(fromKeys, Mode.VISUAL)
}

fun VimScope.ounmap(fromKeys: String) {
  removeMapping(fromKeys, Mode.OP_PENDING)
}

fun VimScope.iunmap(fromKeys: String) {
  removeMapping(fromKeys, Mode.INSERT)
}

fun VimScope.cunmap(fromKeys: String) {
  removeMapping(fromKeys, Mode.COMMAND)
}

private fun VimScope.addMapping(fromKeys: String, toKeys: String, isRecursive: Boolean, vararg modes: Mode) {
  vimPluginApi.addMapping(this, fromKeys, toKeys, isRecursive, *modes)
}

private fun VimScope.addMapping(
  fromKeys: String,
  isRecursive: Boolean,
  isRepeatable: Boolean,
  action: VimScope.() -> Unit,
  vararg modes: Mode,
) {
  vimPluginApi.addMapping(fromKeys, this, isRecursive, isRepeatable, action, *modes)
}

private fun VimScope.removeMapping(fromKeys: String, vararg modes: Mode) {
  vimPluginApi.removeMapping(this, fromKeys, *modes)
}