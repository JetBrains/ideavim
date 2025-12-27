/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.listFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction(name = "index")
internal class IndexFunctionHandler : BuiltinFunctionHandler<VimInt>(minArity = 2, maxArity = 4) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimInt {
    val obj = arguments[0]
    val expr = arguments[1]
    val start = arguments.getNumberOrNull(2)?.value ?: 0
    val ic = arguments.getNumberOrNull(3)?.booleanValue ?: false

    if (obj !is VimList) {
      return VimInt.MINUS_ONE
    }

    val startIndex = if (start < 0) {
      (obj.values.size + start).coerceAtLeast(0)
    } else {
      start.coerceAtMost(obj.values.size)
    }

    for (i in startIndex until obj.values.size) {
      val item = obj.values[i]
      if (item.valueEquals(expr, ic)) {
        return VimInt(i)
      }
    }

    return VimInt.MINUS_ONE
  }
}
