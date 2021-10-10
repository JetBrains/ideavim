package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

abstract class BinaryOperatorWithIgnoreCaseOption(
  private val caseInsensitiveImpl: BinaryOperatorHandler,
  private val caseSensitiveImpl: BinaryOperatorHandler,
) : BinaryOperatorHandler() {

  private fun shouldIgnoreCase(): Boolean {
    return (OptionsManager.getOption("ignorecase") as ToggleOption).isSet
  }

  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (shouldIgnoreCase()) caseInsensitiveImpl.performOperation(left, right) else
      caseSensitiveImpl.performOperation(left, right)
  }
}
