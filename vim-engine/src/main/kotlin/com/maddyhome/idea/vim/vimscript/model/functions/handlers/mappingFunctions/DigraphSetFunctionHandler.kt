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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.functions.BinaryFunctionHandler

@VimscriptFunction("digraph_set")
internal class DigraphSetFunctionHandler : BinaryFunctionHandler<VimInt>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimInt {
    val chars = arguments.getString(0).value
    val digraph = arguments.getString(1).value

    if (chars.length != 2) {
      throw exExceptionMessage("E1214", chars)
    }

    if (digraph.codePointCount(0, digraph.length) != 1) {
      throw exExceptionMessage("E1215", digraph)
    }

    injector.digraphGroup.setDigraph(chars, digraph.codePointAt(0))
    return VimInt.ONE
  }
}
