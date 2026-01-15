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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler

@VimscriptFunction(name = "reverse")
internal class ReverseFunctionHandler : UnaryFunctionHandler<VimDataType>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val argument = arguments[0]
    return when (argument) {
      is VimList -> {
        if (argument.isLocked) {
          throw exExceptionMessage("E741", "reverse() argument")
        }
        argument.also { it.values.reverse() }
      }

      is VimString -> {
        if (argument.isLocked) {
          throw exExceptionMessage("E741", "reverse() argument")
        }
        VimString(argument.value.reversed())
      }

      is VimBlob -> TODO()
      else -> throw exExceptionMessage("E1252", 1)
    }
  }
}
