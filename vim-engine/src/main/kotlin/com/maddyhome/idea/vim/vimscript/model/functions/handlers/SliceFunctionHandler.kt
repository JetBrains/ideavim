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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandlerBase

@VimscriptFunction(name = "slice")
internal class SliceFunctionHandler : FunctionHandlerBase<VimDataType>(minArity = 2, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val expr = arguments[0]
    val start = arguments.getNumber(1).value
    val endExclusive = if (arguments.size == 3) arguments.getNumber(2).value else null

    return when (expr) {
      is VimList -> expr.slice(start, endExclusive ?: expr.values.size)
      is VimString -> expr.substring(start, endExclusive ?: expr.value.length)
      is VimInt -> expr.toVimString().let {
        it.substring(start, endExclusive ?: it.value.length)
      }
      is VimDictionary -> expr
      else -> VimInt.ZERO
    }
  }
}
