/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

internal object IsNotHandler : BinaryOperatorWithIgnoreCaseOption(IsNotIgnoreCaseHandler, IsNotCaseSensitiveHandler)

internal object IsNotIgnoreCaseHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (left is VimString && right is VimString) {
      VimInt(if (left.value.compareTo(right.value, ignoreCase = true) != 0) 1 else 0)
    } else {
      VimInt(if (left != right) 1 else 0)
    }
  }
}

internal object IsNotCaseSensitiveHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return VimInt(if (left != right) 1 else 0)
  }
}
