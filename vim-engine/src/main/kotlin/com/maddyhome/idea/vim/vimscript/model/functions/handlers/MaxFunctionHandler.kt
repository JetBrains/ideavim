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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

@VimscriptFunction(name = "max")
internal class MaxFunctionHandler : FunctionHandler() {
  override val minimumNumberOfArguments = 1
  override val maximumNumberOfArguments = 1

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val expr = argumentValues[0].evaluate(editor, context, vimContext)

    val values = when (expr) {
      is VimList -> expr.values
      is VimDictionary -> expr.dictionary.values.toList()
      else -> throw exExceptionMessage("E712", "max()")
    }

    // Empty list/dict returns 0
    if (values.isEmpty()) {
      return VimInt(0)
    }

    return try {
      val maxValue = values.maxOf { it.toVimNumber().value }
      VimInt(maxValue)
    } catch (_: Exception) {
      throw exExceptionMessage("E712", "max()")
    }
  }
}
