/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

internal object ModulusHandler : BinaryOperatorHandler() {

  private fun modulus(l: Int, r: Int): Int {
    return if (r == 0) 0 else l % r
  }

  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    if (left is VimFloat || right is VimFloat) {
      throw ExException("E804: Connot use '%' with Float")
    } else {
      return VimInt(modulus(left.asDouble().toInt(), right.asDouble().toInt()))
    }
  }
}
