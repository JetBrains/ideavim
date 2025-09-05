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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

data class Variable(val scope: Scope?, val name: CurlyBracesName) : Expression() {
  constructor(scope: Scope?, name: String) : this(scope, CurlyBracesName(listOf(SimpleExpression(name))))

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    return injector.variableService.getNonNullVariableValue(this, editor, context, vimContext)
  }

  fun toString(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): String {
    return (scope?.toString() ?: "") + name.evaluate(editor, context, vimContext).value
  }
}
