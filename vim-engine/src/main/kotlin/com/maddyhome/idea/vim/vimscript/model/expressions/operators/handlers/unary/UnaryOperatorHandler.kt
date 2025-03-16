/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions.operators.handlers.unary

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

internal abstract class UnaryOperatorHandler {
  abstract fun performOperation(value: VimDataType): VimDataType
}
