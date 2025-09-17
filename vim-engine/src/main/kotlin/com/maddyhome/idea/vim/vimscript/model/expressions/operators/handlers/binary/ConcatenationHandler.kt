/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.binary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFloat
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

internal object ConcatenationHandler : BinaryOperatorHandler() {
  override fun performOperation(left: VimDataType, right: VimDataType): VimString {
    // Concatenation allows converting Float to String
    val l = if (left is VimFloat) left.toOutputString() else left.toVimString().value
    val r = if (right is VimFloat) right.toOutputString() else right.toVimString().value
    return VimString(l + r)
  }
}
