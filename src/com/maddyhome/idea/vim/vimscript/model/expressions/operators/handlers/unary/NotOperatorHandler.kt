package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt

object NotOperatorHandler : UnaryOperatorHandler() {

  override fun performOperation(value: VimDataType): VimDataType {
    return (!value.asBoolean()).asVimInt()
  }
}
