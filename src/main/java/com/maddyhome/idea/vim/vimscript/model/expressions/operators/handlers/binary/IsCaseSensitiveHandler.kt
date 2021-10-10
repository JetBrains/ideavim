package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

object IsCaseSensitiveHandler : BinaryOperatorHandler() {

  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return VimInt(if (left == right) 1 else 0)
  }
}
