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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler

@VimscriptFunction("digraph_setlist")
internal class DigraphSetListFunctionHandler : UnaryFunctionHandler<VimInt>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimInt {
    val list = arguments[0] as? VimList ?: throw exExceptionMessage("E1216")
    if (list.values.any { it !is VimList || it.size != 2 }) {
      throw exExceptionMessage("E1216")
    }

    list.values.forEach {
      val chars = ((it as VimList).values[0]).toVimString().value // TODO: We don't get the exception about converting Float to String
      if (chars.length != 2) {
        throw exExceptionMessage("E1214", chars)
      }

      val digraph = it.values[1].toVimString().value
      if (digraph.codePointCount(0, digraph.length) != 1) {
        throw exExceptionMessage("E1215", digraph)
      }

      injector.digraphGroup.setDigraph(chars, digraph.codePointAt(0))
    }

    return VimInt.ONE
  }
}
