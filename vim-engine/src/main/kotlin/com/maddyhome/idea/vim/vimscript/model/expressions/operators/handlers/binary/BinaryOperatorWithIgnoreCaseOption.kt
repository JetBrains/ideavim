/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

abstract class BinaryOperatorWithIgnoreCaseOption(
  private val caseInsensitiveImpl: BinaryOperatorHandler,
  private val caseSensitiveImpl: BinaryOperatorHandler,
) : BinaryOperatorHandler() {

  private fun shouldIgnoreCase(): Boolean {
    return injector.optionService.isSet(OptionScope.GLOBAL, OptionConstants.ignorecase)
  }

  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (shouldIgnoreCase()) caseInsensitiveImpl.performOperation(left, right) else
      caseSensitiveImpl.performOperation(left, right)
  }
}
