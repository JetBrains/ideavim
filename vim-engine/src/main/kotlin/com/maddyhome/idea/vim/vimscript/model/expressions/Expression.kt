/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

abstract class Expression {

  lateinit var originalString: String
  abstract fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType
}

/**
 * An expression that can act as an lvalue, and be assigned a new value
 */
abstract class LValueExpression : Expression() {
  /**
   * Returns true if this expression is strongly typed
   *
   * Register expressions are strongly typed, as registers are always strings. Options are strongly typed too, either
   * String or Number. Variables, indexed expressions and sublists are not strongly typed, as any value can be assigned
   * to them.
   */
  abstract fun isStronglyTyped(): Boolean

  abstract fun assign(value: VimDataType, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext)
}
