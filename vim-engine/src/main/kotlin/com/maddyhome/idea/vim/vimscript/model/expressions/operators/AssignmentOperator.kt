/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
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
        // in this case we should update existing list instead of creating a new one
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
}
