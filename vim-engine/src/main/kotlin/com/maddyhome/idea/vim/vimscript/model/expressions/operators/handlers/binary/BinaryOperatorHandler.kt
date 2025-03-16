/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat

internal abstract class BinaryOperatorHandler {
  abstract fun performOperation(left: VimDataType, right: VimDataType): VimDataType

  /**
   * Coerce a Vim value to a Float
   *
   * Typically, Vim only automatically converts between String and Number. That is, you can call `abs("-2")` and Vim
   * will convert the String argument to Number. However, when evaluating a binary operator, both sides of the operator
   * need to be the same type, e.g., List and List, Dictionary and Dictionary. Vim will still automatically convert
   * between String and Number, but for operators, it will also convert from Number to Float. String will therefore also
   * convert to Float, although via Number, so `"1.5"` becomes `1.0`.
   *
   * This function will try to convert the given value to Number and return the double value of the integer value. If
   * the value isn't Number or String, an [ExException] is thrown.
   */
  protected fun coerceToVimFloatValue(value: VimDataType) =
    if (value is VimFloat) value.value else value.toVimNumber().value.toDouble()
}
