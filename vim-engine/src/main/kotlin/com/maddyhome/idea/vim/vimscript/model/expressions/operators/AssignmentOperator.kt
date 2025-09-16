/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators

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

  fun getNewValue(left: VimDataType?, right: VimDataType): VimDataType {
    return when (this) {
      ASSIGNMENT -> right
      ADDITION -> {
        // in this case we should update the existing list instead of creating a new one
        if (left is VimList && right is VimList) {
          left.values.addAll(right.values)
          left
        } else {
          AdditionHandler.performOperation(left!!, right)
        }
      }

      SUBTRACTION -> SubtractionHandler.performOperation(left!!, right)
      MULTIPLICATION -> MultiplicationHandler.performOperation(left!!, right)
      DIVISION -> DivisionHandler.performOperation(left!!, right)
      MODULUS -> ModulusHandler.performOperation(left!!, right)
      CONCATENATION -> ConcatenationHandler.performOperation(left!!, right)
    }
  }

  /**
   * Given a strongly typed lvalue, is the operator valid for that type?
   *
   * If the lvalue of a compound operator is strongly typed, then only certain operators make sense for that type. For
   * example, a register is a strongly typed String lvalue, so arithmetic operators don't make sense. Same for a String
   * option. Likewise, a Number option can only use arithmetic operators. An untyped lvalue, such as a variable, indexed
   * expression, or sublist, doesn't have any restrictions on what operators make sense, and the operator will attempt
   * to convert between String, Number, and Float as necessary. The resulting value is assigned to the lvalue, and it
   * takes the type of the result.
   *
   * Note that an arithmetic operator can convert String to Number and coerce Number to Float if one side is Float.
   * The resulting Number or Float is assigned to an untyped lvalue. An arithmetic operator that coerces to Float for
   * a typed lvalue will fail when trying to assign the result (e.g. `E806: Using a Float as a Number`).
   *
   * The simple assignment operator does not operate on an lvalue, so doesn't care what type it is. It doesn't
   * transform, but can still fail when trying to assign a value of a different type to a strongly typed lvalue.
   */
  fun isApplicableToType(value: VimDataType) = when (this) {
    ASSIGNMENT -> true  // The operator doesn't care about the lvalue type, but the lvalue might
    CONCATENATION -> value is VimString
    else -> value is VimInt || value is VimFloat
  }
}
