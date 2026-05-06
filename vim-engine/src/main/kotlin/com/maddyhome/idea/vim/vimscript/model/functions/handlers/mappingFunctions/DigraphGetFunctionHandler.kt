/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.mappingFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimString
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler

@VimscriptFunction("digraph_get")
internal class DigraphGetFunctionHandler : UnaryFunctionHandler<VimString>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimString {
    val chars = arguments.getString(0).value

    if (chars.length != 2) {
      throw exExceptionMessage("E1214", chars)
    }

    val codepoint =  injector.digraphGroup.getCharacterForDigraph(chars[0], chars[1])
    return String(intArrayOf(codepoint), 0, 1).asVimString()
  }
}
