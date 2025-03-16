/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt

internal abstract class ArithmeticUnaryOperatorHandler : UnaryOperatorHandler() {
  final override fun performOperation(value: VimDataType): VimDataType {
    return if (value is VimFloat) {
      VimFloat(performOperation(value.value))
    } else {
      performOperation(value.toVimNumber().value).asVimInt()
    }
  }

  protected abstract fun performOperation(value: Double) : Double
  protected abstract fun performOperation(value: Int) : Int
}

internal object MinusOperatorHandler : ArithmeticUnaryOperatorHandler() {
  override fun performOperation(value: Double) = -value
  override fun performOperation(value: Int) = -value
}

internal object PlusOperatorHandler : ArithmeticUnaryOperatorHandler() {
  override fun performOperation(value: Double) = value
  override fun performOperation(value: Int) = value
}
