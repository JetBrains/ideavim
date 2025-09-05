/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

import com.maddyhome.idea.vim.ex.exExceptionMessage

data class VimString(val value: String) : VimDataType() {

  override fun toVimFloat(): VimFloat {
    throw exExceptionMessage("E892")  // E892: Using a String as a Float
  }

  override fun toVimNumber(): VimInt {
    // Vim will automatically convert a String to a Number when required during evaluation. The value is not parsed as
    // strictly as the text used to create a Number expression - Vim allows trailing characters for Number, so something
    // like `6bar` is parsed as `6`.
    return VimInt.parseNumber(value, allowTrailingCharacters = true) ?: VimInt.ZERO
  }

  override fun toVimString() = this
  override fun toOutputString() = value

  override fun deepCopy(level: Int): VimString {
    return copy()
  }

  override fun lockVar(depth: Int) {
    this.isLocked = true
  }

  override fun unlockVar(depth: Int) {
    this.isLocked = false
  }

  companion object {
    val EMPTY: VimString = VimString("")
  }
}
