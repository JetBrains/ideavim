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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction(name = "range")
internal class RangeFunctionHandler : BuiltinFunctionHandler<VimList>(minArity = 1, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimList {
    val expr = arguments.getNumber(0).value

    if (arguments.size == 1) {
      // range(n) produces [0, 1, ..., n-1]
      if (expr <= 0) {
        return VimList(mutableListOf())
      }
      val result = (0 until expr).map<Int, VimDataType> { VimInt(it) }.toMutableList()
      return VimList(result)
    }

    val max = arguments.getNumber(1).value
    val stride = if (arguments.size > 2) {
      val s = arguments.getNumber(2).value
      // Check for invalid stride
      if (s == 0) {
        throw exExceptionMessage("E726")
      }
      s
    } else {
      1  // Default stride is always 1
    }

    // Check for invalid range (max more than one before start)
    if ((stride > 0 && max < expr - 1) || (stride < 0 && max > expr + 1)) {
      throw exExceptionMessage("E727")
    }

    // When maximum is one before start, return empty list
    if (max == expr - 1) {
      return VimList(mutableListOf())
    }

    val result = mutableListOf<VimDataType>()
    var current = expr
    if (stride > 0) {
      while (current <= max) {
        result.add(VimInt(current))
        current += stride
      }
    } else {
      while (current >= max) {
        result.add(VimInt(current))
        current += stride
      }
    }

    return VimList(result)
  }
}
