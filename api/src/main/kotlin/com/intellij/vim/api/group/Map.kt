/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.group

import com.intellij.vim.api.Api
import com.intellij.vim.api.Mode
import com.intellij.vim.api.Read
import com.intellij.vim.api.Scope

private lateinit var api: Api

fun Scope.addMapping(from: String, to: String, isRecursive: Boolean, vararg modes: Mode) {
  api.addMapping(this, from, to, isRecursive, *modes)
}
fun Scope.map(from: String, to: String) {
  addMapping(from, to, isRecursive = true, Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING)
}
fun Scope.nmap(from: String, to: String) {
  addMapping(from, to, isRecursive = true, Mode.NORMAL)
}
fun Scope.vmap(from: String, to: String) {
  addMapping(from, to, isRecursive = true, Mode.VISUAL)
}
fun Scope.omap(from: String, to: String) {
  addMapping(from, to, isRecursive = true, Mode.OP_PENDING)
}
fun Scope.imap(from: String, to: String) {
  addMapping(from, to, isRecursive = true, Mode.INSERT)
}
fun Scope.cmap(from: String, to: String) {
  addMapping(from, to, isRecursive = true, Mode.COMMAND)
}
fun Scope.nnoremap(from: String, to: String) {
  addMapping(from, to, isRecursive = false, Mode.NORMAL)
}
fun Scope.vnoremap(from: String, to: String) {
  addMapping(from, to, isRecursive = false, Mode.VISUAL)
}
fun Scope.onoremap(from: String, to: String) {
  addMapping(from, to, isRecursive = false, Mode.OP_PENDING)
}
fun Scope.inoremap(from: String, to: String) {
  addMapping(from, to, isRecursive = false, Mode.INSERT)
}
fun Scope.cnoremap(from: String, to: String) {
  addMapping(from, to, isRecursive = false, Mode.COMMAND)
}
fun Scope.noremap(from: String, to: String) {
  addMapping(from, to, isRecursive = false, Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING)
}

fun Scope.addMapping(from: String, vararg modes: Mode, block: ExtensionHandler) {
  api.addMapping(this, from, modes = modes, block = block)
}
fun Scope.map(from: String, block: ExtensionHandler) {
  addMapping(from, Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING, block = block)
}
fun Scope.nmap(from: String, block: ExtensionHandler) {
  addMapping(from, Mode.NORMAL, block = block)
}
fun Scope.vmap(from: String, block: ExtensionHandler) {
  addMapping(from, Mode.VISUAL, block = block)
}
fun Scope.omap(from: String, block: ExtensionHandler) {
  addMapping(from, Mode.OP_PENDING, block = block)
}
fun Scope.imap(from: String, block: ExtensionHandler) {
  addMapping(from, Mode.INSERT, block = block)
}
fun Scope.cmap(from: String, block: ExtensionHandler) {
  addMapping(from, Mode.COMMAND, block = block)
}

fun Scope.removeMapping(from: String, vararg modes: Mode) {
  api.removeMapping(this, from, *modes)
}
fun Scope.unmap(from: String) {
  removeMapping(from, Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING)
}
fun Scope.nunmap(from: String) {
  removeMapping(from, Mode.NORMAL)
}
fun Scope.vunmap(from: String) {
  removeMapping(from, Mode.VISUAL)
}
fun Scope.ounmap(from: String) {
  removeMapping(from, Mode.OP_PENDING)
}
fun Scope.iunmap(from: String) {
  removeMapping(from, Mode.INSERT)
}
fun Scope.cunmap(from: String) {
  removeMapping(from, Mode.COMMAND)
}

// TODO documentation, better name
interface ExtensionHandler {
  // TODO documentation
  val isRepeatable: Boolean
  // TODO documentation
  fun Read.execute()
}
