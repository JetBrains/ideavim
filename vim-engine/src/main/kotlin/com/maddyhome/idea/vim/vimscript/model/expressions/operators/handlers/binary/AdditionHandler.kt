/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList

internal object AdditionHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (left is VimFloat || right is VimFloat) {
      VimFloat(left.asDouble() + right.asDouble())
    } else if (left is VimList && right is VimList) {
      val newList = ArrayList(left.values)
      newList.addAll(right.values)
      VimList(newList)
    } else {
      VimInt((left.asDouble() + right.asDouble()).toInt())
    }
  }
}
