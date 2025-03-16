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

internal object UnequalsHandler : BinaryOperatorWithIgnoreCaseOption(UnequalsIgnoreCaseHandler, UnequalsCaseSensitiveHandler)

internal object UnequalsIgnoreCaseHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (left is VimString && right is VimString) {
      VimInt(if (left.asString().compareTo(right.asString(), ignoreCase = true) != 0) 1 else 0)
    } else {
      VimInt(if (left.asDouble() != right.asDouble()) 1 else 0)
    }
  }
}

internal object UnequalsCaseSensitiveHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return if (left is VimString && right is VimString) {
      VimInt(if (left.asString() != right.asString()) 1 else 0)
    } else {
      VimInt(if (left.asDouble() != right.asDouble()) 1 else 0)
    }
  }
}
