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
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimString
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction("digraph_getlist")
internal class DigraphGetListFunctionHandler : BuiltinFunctionHandler<VimList>(minArity = 0, maxArity = 1) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimList {
    val listAll = arguments.getNumberOrNull(0)?.booleanValue ?: false

    val digraphs = if (listAll) {
      injector.digraphGroup.getAllDigraphs()
    } else {
      injector.digraphGroup.getCustomDigraphs()
    }
    val list = mutableListOf<VimDataType>()
    digraphs.forEach { (chars, codepoint) ->
      list.add(VimList(mutableListOf(chars.asVimString(), String(intArrayOf(codepoint), 0, 1).asVimString())))
    }
    return VimList(list)
  }
}
