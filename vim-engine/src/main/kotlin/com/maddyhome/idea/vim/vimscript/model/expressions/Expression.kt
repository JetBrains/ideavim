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

  /**
   * Assign a new value to this expression
   *
   * When assigning a new value to this expression, the caller must pass the text of the assignment operation, to be
   * used in error messages. If this isn't passed we only have the expression itself, not the operation, which is what
   * Vim displays.
   *
   * @param value The new value to assign
   * @param editor The editor used during assignment (e.g., saving to buffer or window local options)
   * @param context The execution context used during assignment (e.g., to get access to host functionality)
   * @param vimContext The Vimscript context to use during assignment (e.g., to get default variable scope)
   * @param assignmentTextForErrors The text of the assignment operation, to be used in error messages.
   */
  abstract fun assign(
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
    assignmentTextForErrors: String,
  )
}
