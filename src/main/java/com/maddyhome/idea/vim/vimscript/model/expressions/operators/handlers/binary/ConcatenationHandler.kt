package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

object ConcatenationHandler : BinaryOperatorHandler() {

  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return VimString(left.asString() + right.asString())
  }
}
