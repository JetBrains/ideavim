package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

abstract class UnaryOperatorHandler {

  abstract fun performOperation(value: VimDataType): VimDataType
}
