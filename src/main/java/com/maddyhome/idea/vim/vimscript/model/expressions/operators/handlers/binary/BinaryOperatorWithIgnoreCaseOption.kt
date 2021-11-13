package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.services.OptionService

abstract class BinaryOperatorWithIgnoreCaseOption(
  private val caseInsensitiveImpl: BinaryOperatorHandler,
  private val caseSensitiveImpl: BinaryOperatorHandler,
) : BinaryOperatorHandler() {

  private fun shouldIgnoreCase(): Boolean {
    return VimPlugin.getOptionService().isSet(OptionService.Scope.GLOBAL, "ignorecase", null)
  }

  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (shouldIgnoreCase()) caseInsensitiveImpl.performOperation(left, right) else
      caseSensitiveImpl.performOperation(left, right)
  }
}
