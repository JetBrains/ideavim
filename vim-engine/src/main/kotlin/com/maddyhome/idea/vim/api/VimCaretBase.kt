/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.register.Register
import com.maddyhome.idea.vim.register.RegisterConstants
import com.maddyhome.idea.vim.register.VimRegisterGroupBase
import javax.swing.KeyStroke

abstract class VimCaretBase : VimCaret

open class CaretRegisterStorageBase : CaretRegisterStorage, VimRegisterGroupBase() {
  override var lastRegisterChar: Char
    get() {
      return injector.registerGroup.lastRegisterChar
    }
    set(_) {}

  override fun storeText(caret: ImmutableVimCaret, editor: VimEditor, range: TextRange, type: SelectionType, isDelete: Boolean): Boolean {
    if (caret.isPrimary) {
      return injector.registerGroup.storeText(editor, caret, range, type, isDelete)
    }
    val register = lastRegisterChar
    if (!RegisterConstants.RECORDABLE_REGISTERS.contains(register)) {
      return false
    }
    return super.storeText(editor, caret, range, type, isDelete)
  }

  override fun getRegister(caret: ImmutableVimCaret, r: Char): Register? {
    if (caret.isPrimary || !RegisterConstants.RECORDABLE_REGISTERS.contains(r)) {
      return injector.registerGroup.getRegister(r)
    }
    return super.getRegister(r) ?: injector.registerGroup.getRegister(r)
  }

  override fun setKeys(caret: ImmutableVimCaret, register: Char, keys: List<KeyStroke>) {
    if (caret.isPrimary) {
      injector.registerGroup.setKeys(register, keys)
    }
    if (!RegisterConstants.RECORDABLE_REGISTERS.contains(register)) {
      return
    }
    return super.setKeys(register, keys)
  }

  override fun saveRegister(caret: ImmutableVimCaret, r: Char, register: Register) {
    if (caret.isPrimary) {
      injector.registerGroup.saveRegister(r, register)
    }
    if (!RegisterConstants.RECORDABLE_REGISTERS.contains(r)) {
      return
    }
    return super.saveRegister(r, register)
  }
}
