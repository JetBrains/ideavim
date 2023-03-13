/*
 * Copyright 2003-2023 The IdeaVim authors
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

public abstract class VimCaretBase : VimCaret

public open class CaretRegisterStorageBase(override var caret: ImmutableVimCaret) : CaretRegisterStorage, VimRegisterGroupBase() {
  override var lastRegisterChar: Char
    get() {
      return injector.registerGroup.lastRegisterChar
    }
    set(_) {}

  override var isRegisterSpecifiedExplicitly: Boolean
    get() {
      return injector.registerGroup.isRegisterSpecifiedExplicitly
    }
    set(_) {}

  override fun storeText(editor: VimEditor, range: TextRange, type: SelectionType, isDelete: Boolean): Boolean {
    val registerChar = if (caret.editor.carets().size == 1) currentRegister else getCurrentRegisterForMulticaret()
    if (caret.isPrimary) {
      val registerService = injector.registerGroup
      registerService.lastRegisterChar = registerChar
      return registerService.storeText(editor, caret, range, type, isDelete)
    } else {
      if (!RegisterConstants.RECORDABLE_REGISTERS.contains(registerChar)) {
        return false
      }
      val text = preprocessTextBeforeStoring(editor.getText(range), type)
      return storeTextInternal(editor, caret, range, text, type, registerChar, isDelete)
    }
  }

  override fun getRegister(r: Char): Register? {
    if (caret.isPrimary || !RegisterConstants.RECORDABLE_REGISTERS.contains(r)) {
      return injector.registerGroup.getRegister(r)
    }
    return super.getRegister(r) ?: injector.registerGroup.getRegister(r)
  }

  override fun setKeys(register: Char, keys: List<KeyStroke>) {
    if (caret.isPrimary) {
      injector.registerGroup.setKeys(register, keys)
    }
    if (!RegisterConstants.RECORDABLE_REGISTERS.contains(register)) {
      return
    }
    return super.setKeys(register, keys)
  }

  override fun saveRegister(r: Char, register: Register) {
    if (caret.isPrimary) {
      injector.registerGroup.saveRegister(r, register)
    }
    if (!RegisterConstants.RECORDABLE_REGISTERS.contains(r)) {
      return
    }
    return super.saveRegister(r, register)
  }
}
