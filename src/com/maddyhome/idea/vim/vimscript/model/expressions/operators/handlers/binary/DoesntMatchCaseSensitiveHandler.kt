package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.services.PatternService

object DoesntMatchCaseSensitiveHandler : BinaryOperatorHandler() {

  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return PatternService.matches(left.asString(), right.asString(), ignoreCase = false).asVimInt()
  }
}
