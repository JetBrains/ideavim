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

internal object LogicalAndHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimDataType {
    return VimInt(if (left.asDouble() != 0.0 && right.asDouble() != 0.0) 1 else 0)
  }
}
