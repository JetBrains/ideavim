/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.varFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.toVimFuncref

// Unexpectedly, Vim's docs put this in `:help list-functions`. It's got nothing to do with Lists. We'll put it with the
// `:help var-functions` group.`
@VimscriptFunction("call")
internal class CallFunctionHandler : BuiltinFunctionHandler<VimDataType>(minArity = 2, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    range: LineRange?,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val func= arguments[0]  // String or Funcref
    val argList = arguments[1] as? VimList ?: throw exExceptionMessage("E1211", 2)
    val dict = arguments.getOrNull(2)?.let {
      it as? VimDictionary ?: throw exExceptionMessage("E1206", 3)
    }

    var funcref = func.toVimFuncref(editor, context, vimContext)
    if (dict != null && (funcref.dictionary == null || funcref.isImplicitPartial)) {
      funcref = funcref.apply(dict)
    }

    // Execute will set up all the local variables needed
    val args = argList.values.map { SimpleExpression(it) }
    return funcref.execute(args, range = null, editor, context, vimContext)
  }

  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext
  ): VimDataType {
    error("Not implemented")
  }
}
