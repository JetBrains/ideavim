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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt

internal abstract class ComparisonOperatorHandler(ignoreCase: Boolean?) :
  BinaryOperatorWithIgnoreCaseOption(ignoreCase) {

  final override fun performOperation(left: VimDataType, right: VimDataType, ignoreCase: Boolean): VimDataType {
    return when {
      left is VimList || right is VimList -> {
        val leftList = left as? VimList ?: throw exExceptionMessage("E691")
        val rightList = right as? VimList ?: throw exExceptionMessage("E691")
        compare(leftList, rightList, ignoreCase)
      }

      left is VimDictionary || right is VimDictionary -> {
        val leftDictionary = left as? VimDictionary
          ?: throw exExceptionMessage("E735")
        val rightDictionary = right as? VimDictionary
          ?: throw exExceptionMessage("E735")
        compare(leftDictionary, rightDictionary, ignoreCase)
      }

      left is VimFuncref || right is VimFuncref -> {
        // There doesn't appear to be validation on Funcref comparisons, but Vim returns false if the types don't match
        val leftFuncref = left as? VimFuncref
        val rightFuncref = right as? VimFuncref
        if (leftFuncref != null && rightFuncref != null) compare(leftFuncref, rightFuncref) else false
      }

      // TODO: Handle Blob. Presumably both sides must be Blob

      left is VimFloat || right is VimFloat -> {
        val leftFloat = coerceToVimFloatValue(left)
        val rightFloat = coerceToVimFloatValue(right)
        compare(leftFloat, rightFloat)
      }

      left is VimString || right is VimString -> {
        compare(left.toVimString().value, right.toVimString().value, ignoreCase)
      }

      left is VimInt || right is VimInt -> {
        compare(left.toVimNumber().value, right.toVimNumber().value)
      }

      else -> throw exExceptionMessage("E474")
    }.asVimInt()
  }

  protected abstract fun compare(left: Double, right: Double): Boolean
  protected abstract fun compare(left: Int, right: Int): Boolean
  protected abstract fun compare(left: String, right: String, ignoreCase: Boolean): Boolean
  protected open fun compare(left: VimList, right: VimList, ignoreCase: Boolean): Boolean =
    throw exExceptionMessage("E692")
  protected open fun compare(left: VimDictionary, right: VimDictionary, ignoreCase: Boolean): Boolean =
    throw exExceptionMessage("E736")
  protected open fun compare(left: VimFuncref, right: VimFuncref): Boolean =
    throw exExceptionMessage("E694")
}
