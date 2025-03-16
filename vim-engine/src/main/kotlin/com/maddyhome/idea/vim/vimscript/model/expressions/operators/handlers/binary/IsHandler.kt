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

internal object IsHandler : BinaryOperatorWithIgnoreCaseOption(IsIgnoreCaseHandler, IsCaseSensitiveHandler)

internal object IsIgnoreCaseHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return when (left) {
      // Check the value is the same with simple equals. Vim does not convert between Number and String!
      is VimFloat -> left == right
      is VimInt -> left == right

      // Ignore case
      is VimString -> {
        if (right is VimString) left.value.compareTo(right.value, ignoreCase = true) == 0 else false
      }

      // Check the instance is the same with reference equals
      is VimList -> left === right
      is VimDictionary -> left === right
      is VimFuncref -> left === right
      is VimBlob -> left === right
      else -> false
    }.asVimInt()
  }
}

internal object IsCaseSensitiveHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return when (left) {
      // Check the value is the same with simple equals. Vim does not convert between Number and String!
      is VimFloat -> left == right
      is VimInt -> left == right
      is VimString -> left == right

      // Check the instance is the same with reference equals
      is VimList -> left === right
      is VimDictionary -> left === right
      is VimFuncref -> left === right
      is VimBlob -> left === right
      else -> false
    }.asVimInt()
  }
}
