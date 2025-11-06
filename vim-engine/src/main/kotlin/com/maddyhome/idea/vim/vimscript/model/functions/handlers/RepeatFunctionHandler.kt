/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

@VimscriptFunction(name = "repeat")
internal class RepeatFunctionHandler : FunctionHandler() {
  override val minimumNumberOfArguments = 2
  override val maximumNumberOfArguments = 2

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val expr = argumentValues[0].evaluate(editor, context, vimContext)
    val count = argumentValues[1].evaluate(editor, context, vimContext).toVimNumber().value.coerceAtLeast(0)

    return when (expr) {
      is VimList -> {
        val result = mutableListOf<VimDataType>()
        repeat(count) {
          result.addAll(expr.values)
        }
        VimList(result)
      }
      else -> {
        // For strings and other types, convert to string and repeat
        VimString(expr.toVimString().value.repeat(count))
      }
    }
  }
}
