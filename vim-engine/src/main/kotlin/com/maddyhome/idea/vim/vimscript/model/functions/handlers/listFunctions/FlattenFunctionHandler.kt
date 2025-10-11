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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandlerBase

@VimscriptFunction(name = "flatten")
class FlattenFunctionHandler : FunctionHandlerBase<VimList>(minArity = 1, maxArity = 2) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimList {
    val argument = arguments[0]
    if (argument !is VimList) {
      throw exExceptionMessage("E686", "flatten()")
    }

    val maxDepth = arguments.getNumberOrNull(1)?.value ?: Int.MAX_VALUE
    if (maxDepth < 0) {
      throw exExceptionMessage("E900")
    }

    if (argument.isLocked) {
      throw exExceptionMessage("E741", "flatten() argument")
    }

    if (maxDepth > 0) {
      flatten(argument.values, 0, maxDepth)
    }

    return argument
  }

  private fun flatten(originalList: MutableList<VimDataType>, index: Int, depth: Int) {
    if (depth == 0) return
    var index = index
    while (index < originalList.size) {
      val value = originalList[index]
      if (value is VimList) {
        originalList.removeAt(index)
        // Note that value might be the original list! We have to be careful adding to the original list while also
        // iterating over it.
        originalList.addAll(index, value.values)
        flatten(originalList, index, depth - 1)
        index += value.values.size
      }
      else {
        index++
      }
    }
  }
}
