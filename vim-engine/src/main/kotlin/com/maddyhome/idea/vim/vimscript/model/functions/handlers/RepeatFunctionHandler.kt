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
import com.maddyhome.idea.vim.vimscript.model.functions.BinaryFunctionHandler

@VimscriptFunction(name = "repeat")
internal class RepeatFunctionHandler : BinaryFunctionHandler<VimDataType>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val expr = arguments[0]
    val count = arguments.getNumber(1).value.coerceAtLeast(0)

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
