package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

object IsIgnoreCaseHandler : BinaryOperatorHandler() {

  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (left is VimString && right is VimString) {
      VimInt(if (left.value.compareTo(right.value, ignoreCase = true) == 0) 1 else 0)
    } else {
      VimInt(if (left == right) 1 else 0)
    }
  }
}
