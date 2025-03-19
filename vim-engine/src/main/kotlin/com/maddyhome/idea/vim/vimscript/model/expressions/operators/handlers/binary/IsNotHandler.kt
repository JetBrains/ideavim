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

internal class IsNotHandler(ignoreCase: Boolean? = null) : BinaryOperatorWithIgnoreCaseOption(ignoreCase) {
  override fun performOperation(left: VimDataType, right: VimDataType, ignoreCase: Boolean): VimDataType {
    return when (left) {
      // Check the value is the same with simple equals (data classes). Vim does not convert between Number and String!
      is VimFloat -> left != right
      is VimInt -> left != right

      // Can't use simple equals for case insensitive `isnot`
      is VimString -> {
        if (right is VimString) left.value.compareTo(right.value, ignoreCase = true) != 0 else false
      }

      // Check the instance is the same with reference equals
      is VimList -> left !== right
      is VimDictionary -> left !== right
      is VimFuncref -> left !== right
      is VimBlob -> left !== right
      else -> false
    }.asVimInt()
  }
}
