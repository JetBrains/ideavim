package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList

object AdditionHandler : BinaryOperatorHandler() {

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
