package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

object LessOrEqualsIgnoreCaseHandler : BinaryOperatorHandler() {

  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (left is VimString || right is VimString) {
      VimInt(if (left.asString().compareTo(right.asString(), ignoreCase = true) > 0) 0 else 1)
    } else {
      VimInt(if (left.asDouble() > right.asDouble()) 0 else 1)
    }
  }
}
