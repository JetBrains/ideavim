/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.datatypes

class VimBlob : VimDataType("blob") {
  override fun toVimFloat(): VimFloat {
    TODO("Not yet implemented")
  }

  override fun toVimNumber(): VimInt {
    TODO("Not yet implemented")
  }

  override fun toVimString(): VimString {
    TODO("Not yet implemented")
  }

  override fun deepCopy(level: Int): VimDataType {
    TODO("Not yet implemented")
  }

  override fun lockVar(depth: Int) {
    TODO("Not yet implemented")
  }

  override fun unlockVar(depth: Int) {
    TODO("Not yet implemented")
  }

  override fun toOutputString(): String {
    TODO("Not yet implemented")
  }

  override fun copy(): VimDataType {
    TODO("Not yet implemented")
  }
}
