package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

object SubtractionHandler : BinaryOperatorHandler() {

  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (left is VimFloat || right is VimFloat) {
      VimFloat(left.asDouble() - right.asDouble())
    } else {
      VimInt((left.asDouble() - right.asDouble()).toInt())
    }
  }
}
