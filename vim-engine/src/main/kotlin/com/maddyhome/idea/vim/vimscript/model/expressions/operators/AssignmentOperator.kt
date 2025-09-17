/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators

import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.AdditionHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.ConcatenationHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.DivisionHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.ModulusHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.MultiplicationHandler
import com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary.SubtractionHandler

enum class AssignmentOperator(val value: String) {
  ASSIGNMENT("="),
  ADDITION("+="),
  SUBTRACTION("-="),
  MULTIPLICATION("*="),
  DIVISION("/="),
  MODULUS("%="),
  CONCATENATION(".="),
  ;

  companion object {
    fun getByValue(value: String): AssignmentOperator {
      return entries.first { it.value == value }
    }
  }

  /**
   * Calculate the new value for the lvalue after the operator is applied
   *
   * It is up to the operator to convert the values to the correct or compatible types. Typically, this means that
   * String and Number can be converted in both directions. Vim also allows operators to coerce to Float if one side is
   * Float (this conversion isn't allowed elsewhere, e.g., indexes must be Number, but can be converted from String).
   * The operator may throw exceptions if the conversion is not possible.
   *
   * However, if the lvalue is strongly typed, such as an option or register, then the operator is stricter. For
   * example, a register is a strongly typed String lvalue, so arithmetic operators don't make sense, even though Number
   * and String can be converted. A Number option is strongly typed, and so concatenation is not allowed, even though
   * the concatenation operator allows converting Number and Float to String.
   *
   * @param lvalue The left-hand side of the operator
   * @param rvalue The right-hand side of the operator
   * @param isLValueStronglyTyped Is the lvalue a strongly typed expression (e.g., option or register)?
   * @return The new value for the lvalue after the operator is applied
   */
  fun getNewValue(lvalue: VimDataType?, rvalue: VimDataType, isLValueStronglyTyped: Boolean): VimDataType {
    validateValues(lvalue, rvalue, isLValueStronglyTyped)
    return when (this) {
      ASSIGNMENT -> rvalue
      ADDITION -> {
        // AdditionHandler will create a new list, but compound assignment means we modify the existing list
        if (lvalue is VimList && rvalue is VimList) {
          lvalue.values.addAll(rvalue.values)
          lvalue
        } else {
          AdditionHandler.performOperation(lvalue!!, rvalue)
        }
      }

      SUBTRACTION -> SubtractionHandler.performOperation(lvalue!!, rvalue)
      MULTIPLICATION -> MultiplicationHandler.performOperation(lvalue!!, rvalue)
      DIVISION -> DivisionHandler.performOperation(lvalue!!, rvalue)
      MODULUS -> ModulusHandler.performOperation(lvalue!!, rvalue)
      CONCATENATION -> ConcatenationHandler.performOperation(lvalue!!, rvalue)
    }
  }

  private fun validateValues(lvalue: VimDataType?, rvalue: VimDataType, isLValueStronglyTyped: Boolean) {
    when (this) {
      ASSIGNMENT -> {
        // The operator makes no transformations that need to be validated. The assignment might still fail.
        return
      }

      CONCATENATION -> {
        if (isLValueStronglyTyped && lvalue !is VimString) {
          // Concatenation is only allowed for String lvalues. We'll try to convert to a String unless it's a strongly
          // typed lvalue such as a register or option
          throw exExceptionMessage("E734", value)
        }
        if (lvalue is VimFloat || (rvalue is VimFloat && !isLValueStronglyTyped)) {
          // The concatenation compound assignment operator does not allow converting from Float to String, even though
          // this is allowed for the binary concatenation operator.
          // * `let s=1.5 | let s.='2'` should fail, even though `echo string(1.5 .. '2')` works ('1.52')
          // * `let s='foo' | let s.=20.5` should fail, even though `echo string('foo' .. 20.5)` works ('foo25')
          // However, it is allowed if the lvalue is strongly typed, such as a register or option.
          // * `let &titlestring='hello' | let &titlestring.=20.5` should succeed! => 'hello20.5'
          throw exExceptionMessage("E734", value)
        }
      }

      ADDITION -> {
        if (lvalue is VimList && rvalue is VimList) return
        validateArithmeticOperatorValues(lvalue, rvalue, isLValueStronglyTyped)
      }

      else -> validateArithmeticOperatorValues(lvalue, rvalue, isLValueStronglyTyped)
    }
  }

  private fun validateArithmeticOperatorValues(lvalue: VimDataType?, rvalue: VimDataType, isLValueStronglyTyped: Boolean) {
    // Arithmetic operator. If the lvalue is strongly typed, such as an option or register, then the lvalue must be
    // Number or Float. If it's not strongly typed, the String is also allowed. But nothing else is.
    if ((isLValueStronglyTyped && lvalue !is VimInt && lvalue !is VimFloat)
      || (!isLValueStronglyTyped && lvalue !is VimInt && lvalue !is VimFloat && lvalue !is VimString)){
      throw exExceptionMessage("E734", value)
    }

    // This would be caught when trying to convert to Number or Float, but that reports a different error code
    if (rvalue !is VimInt && rvalue !is VimFloat && rvalue !is VimString) {
      throw exExceptionMessage("E734", value)
    }
  }
}
