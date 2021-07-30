package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

abstract class BinaryOperatorHandler {

  abstract fun performOperation(left: VimDataType, right: VimDataType): VimDataType
}
