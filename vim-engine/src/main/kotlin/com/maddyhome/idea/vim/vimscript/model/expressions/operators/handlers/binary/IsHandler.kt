/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt

internal class IsHandler(ignoreCase: Boolean? = null) : IsHandlerBase(ignoreCase)
internal class IsNotHandler(ignoreCase: Boolean? = null) : IsHandlerBase(ignoreCase) {
  override fun performOperation(left: VimDataType, right: VimDataType, ignoreCase: Boolean): VimDataType {
    val result = super.performOperation(left, right, ignoreCase) as VimInt
    return (!result.booleanValue).asVimInt()
  }
}

internal open class IsHandlerBase(ignoreCase: Boolean?) : BinaryOperatorWithIgnoreCaseOption(ignoreCase) {
  override fun performOperation(left: VimDataType, right: VimDataType, ignoreCase: Boolean): VimDataType {
    return when (left) {
      // Check the value is the same with simple equals. Vim does not convert between Number and String!
      is VimFloat -> left == right
      is VimInt -> left == right

      // Can't do simple equals for case-insensitive `is`
      is VimString -> if (right is VimString) left.value.compareTo(right.value, ignoreCase) == 0 else false

      // Check the instance is the same with reference equals
      is VimList -> left === right
      is VimDictionary -> left === right
      is VimFuncref if (right is VimFuncref) -> {
        // A simple "function" reference (as opposed to a "funcref" reference) is the same, as long as it's not partial
        // TODO: The name check might not be enough once we properly support script-local functions
        if (isSimpleLateBoundFunctionReference(left) && isSimpleLateBoundFunctionReference(right)
          && left.handler.name == right.handler.name
        ) {
          true
        }
        else {
          left === right
        }
      }
      is VimBlob -> left === right
      else -> false
    }.asVimInt()
  }

  private fun isSimpleLateBoundFunctionReference(funcref: VimFuncref) =
    funcref.type == VimFuncref.Type.FUNCTION && !funcref.isPartial
}
