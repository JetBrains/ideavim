package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

object PlusOperatorHandler : UnaryOperatorHandler() {

  override fun performOperation(value: VimDataType): VimDataType {
    return if (value is VimFloat) {
      VimFloat(value.asDouble())
    } else {
      VimInt(value.asDouble().toInt())
    }
  }
}
