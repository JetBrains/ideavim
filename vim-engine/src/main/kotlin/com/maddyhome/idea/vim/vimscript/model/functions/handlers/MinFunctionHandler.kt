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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler

@VimscriptFunction(name = "min")
internal class MinFunctionHandler : UnaryFunctionHandler<VimInt>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimInt {
    val expr = arguments[0]

    val values = when (expr) {
      is VimList -> expr.values
      is VimDictionary -> expr.dictionary.values.toList()
      else -> throw exExceptionMessage("E712", "min()")
    }

    // Empty list/dict returns 0
    if (values.isEmpty()) {
      return VimInt.ZERO
    }

    return try {
      val minValue = values.minOf { it.toVimNumber().value }
      VimInt(minValue)
    } catch (_: Exception) {
      throw exExceptionMessage("E712", "min()")
    }
  }
}
