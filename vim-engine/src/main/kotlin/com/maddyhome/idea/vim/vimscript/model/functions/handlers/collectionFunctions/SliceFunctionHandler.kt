/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.collectionFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction(name = "slice")
internal class SliceFunctionHandler : BuiltinFunctionHandler<VimDataType>(minArity = 2, maxArity = 3) {
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
      is VimList -> expr.slice(start, toEndExclusive(endExclusive, expr.values.size))
      is VimString -> expr.substring(start, toEndExclusive(endExclusive, expr.value.length))
      is VimInt -> expr.toVimString().let {
        it.substring(start, toEndExclusive(endExclusive, it.value.length))
      }
      is VimDictionary -> expr
      else -> VimInt.ZERO
    }
  }

  /**
   * Resolves the `slice()` end index into the absolute exclusive end index used by [VimList.slice]
   *
   * Unlike the sublist expression `expr[start : end]`, the end index of `slice()` is already exclusive, so it only
   * needs resolving against [size] when negative. A missing end index means "to the end of the collection".
   */
  private fun toEndExclusive(endExclusive: Int?, size: Int) = when {
    endExclusive == null -> size
    endExclusive < 0 -> endExclusive + size
    else -> endExclusive
  }
}
