package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

object NotOperatorHandler : UnaryOperatorHandler() {

  override fun performOperation(value: VimDataType): VimDataType {
    return if (!value.asBoolean()) VimInt(1) else VimInt(0)
  }
}
