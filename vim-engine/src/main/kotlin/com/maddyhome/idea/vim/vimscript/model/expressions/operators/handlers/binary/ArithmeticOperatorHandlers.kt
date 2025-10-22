/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt

internal abstract class ArithmeticBinaryOperatorHandler() : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (left is VimFloat || right is VimFloat) {
      val leftFloat = coerceToVimFloatValue(left)
      val rightFloat = coerceToVimFloatValue(right)
      VimFloat(performOperation(leftFloat, rightFloat))
    }
    else {
      val leftNumber = left.toVimNumber().value
      val rightNumber = right.toVimNumber().value
      performOperation(leftNumber, rightNumber).asVimInt()
    }
  }

  protected abstract fun performOperation(left: Double, right: Double): Double
  protected abstract fun performOperation(left: Int, right: Int): Int

  /**
   * Coerce a Vim value to a Float
   *
   * Typically, Vim only automatically converts between String and Number. That is, you can call `abs("-2")` and Vim
   * will convert the String argument to Number. However, when evaluating a binary operator, both sides of the operator
   * need to be the same type, e.g., List and List, Dictionary and Dictionary. Vim will still automatically convert
   * between String and Number, but for operators, it will also convert from Number to Float.
   *
   * For arithmetic operator purposes, a String can be converted to a Number and then converted to a Float. This means
   * it will be a float representation of an integer, so `"1.5"` becomes `1.5`.
   *
   * This function will try to convert the given value to Number and return the double value of the integer value. If
   * the value isn't Number or String, an [ExException] is thrown.
   */
  protected open fun coerceToVimFloatValue(value: VimDataType) =
    if (value is VimFloat) value.value else value.toVimNumber().value.toDouble()
}

internal object AdditionHandler : ArithmeticBinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    if (left is VimList && right is VimList) {
      val newList = ArrayList(left.values)
      newList.addAll(right.values)
      return VimList(newList)
    }

    return super.performOperation(left, right)
  }

  override fun performOperation(left: Double, right: Double) = left + right
  override fun performOperation(left: Int, right: Int) = left + right
}

internal object SubtractionHandler : ArithmeticBinaryOperatorHandler() {
  override fun performOperation(left: Double, right: Double) = left - right
  override fun performOperation(left: Int, right: Int) = left - right
}

internal object MultiplicationHandler : ArithmeticBinaryOperatorHandler() {
  override fun performOperation(left: Double, right: Double) = left * right
  override fun performOperation(left: Int, right: Int) = left * right
}

internal object DivisionHandler : ArithmeticBinaryOperatorHandler() {
  override fun performOperation(left: Double, right: Double) = left / right
  override fun performOperation(left: Int, right: Int): Int {
    // We get an exception when dividing an integer by 0. Doubles give NaN, which becomes 0 when converted to integer
    return (left.toDouble() / right.toDouble()).toInt()
  }
}

internal object ModulusHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    if (left is VimFloat || right is VimFloat) {
      throw exExceptionMessage("E804")
    }

    val leftNumber = left.toVimNumber().value
    val rightNumber = right.toVimNumber().value
    return (if (rightNumber == 0) 0 else leftNumber % rightNumber).asVimInt()
  }
}
