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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction(name = "join")
internal class JoinFunctionHandler : BuiltinFunctionHandler<VimString>(minArity = 1, maxArity = 2) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimString {
    val list = arguments[0]
    if (list !is VimList) {
      throw exExceptionMessage("E1211", "1")
    }

    // Note that the docs state that the values are formatted with Vim's `string()` function, except for String itself.
    // The `string()` function is essentially the same as `toOutputString`, but it adds single quotes to String. We're
    // safe to use `toOutputString` here.
    val separator = arguments.getOrNull(1)?.toVimString()?.value ?: " "
    return VimString(list.values.joinToString(separator) { it.toOutputString() })
  }
}
