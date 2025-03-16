/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList

internal object AdditionHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (left is VimFloat || right is VimFloat) {
      VimFloat(left.asDouble() + right.asDouble())
    } else if (left is VimList && right is VimList) {
      val newList = ArrayList(left.values)
      newList.addAll(right.values)
      VimList(newList)
    } else {
      VimInt((left.asDouble() + right.asDouble()).toInt())
    }
  }
}

internal object SubtractionHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (left is VimFloat || right is VimFloat) {
      VimFloat(left.asDouble() - right.asDouble())
    } else {
      VimInt((left.asDouble() - right.asDouble()).toInt())
    }
  }
}

internal object MultiplicationHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (left is VimFloat || right is VimFloat) {
      VimFloat(left.asDouble() * right.asDouble())
    } else {
      VimInt((left.asDouble() * right.asDouble()).toInt())
    }
  }
}

internal object DivisionHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (left is VimFloat || right is VimFloat) {
      VimFloat(left.asDouble() / right.asDouble())
    } else {
      VimInt((left.asDouble() / right.asDouble()).toInt())
    }
  }
}

internal object ModulusHandler : BinaryOperatorHandler() {
  private fun modulus(l: Int, r: Int): Int {
    return if (r == 0) 0 else l % r
  }

  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    if (left is VimFloat || right is VimFloat) {
      throw exExceptionMessage("E804")  // E804: Cannot use '%' with Float
    } else {
      return VimInt(modulus(left.asDouble().toInt(), right.asDouble().toInt()))
    }
  }
}
