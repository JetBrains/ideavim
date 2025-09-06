/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.listFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandlerBase

@VimscriptFunction(name = "split")
internal class SplitFunctionHandler : FunctionHandlerBase<VimList>(minArity = 1, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ) : VimList {
    val text = arguments.getString(0).value
    val delimiter = arguments.getStringOrNull(1)?.value ?: "\\s\\+"
    val keepEmpty = arguments.getNumberOrNull(2)?.booleanValue ?: false

    val delimiters: List<Pair<Int, Int>> =
      injector.regexpService.getAllMatches(text, delimiter) + Pair(text.length, text.length)
    val result = mutableListOf<String>()
    var startIndex = 0
    for (del in delimiters) {
      if (startIndex != del.first || keepEmpty) result.add(text.substring(startIndex, del.first))
      startIndex = del.second
    }
    return VimList(result.map { VimString(it) }.toMutableList())
  }
}
