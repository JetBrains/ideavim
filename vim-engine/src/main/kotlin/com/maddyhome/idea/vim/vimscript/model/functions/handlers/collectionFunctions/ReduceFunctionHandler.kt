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
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler
import com.maddyhome.idea.vim.vimscript.model.functions.toVimFuncref

@VimscriptFunction("reduce")
internal class ReduceFunctionHandler : BuiltinFunctionHandler<VimDataType>(minArity = 2, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val argument1 = arguments[0]
    val iterable = when (argument1) {
      is VimList -> argument1.values
      is VimString -> argument1.value.map { VimString(it.toString()) }
      is VimBlob -> TODO()
      else -> throw exExceptionMessage("E1098")
    }

    val arg2 = arguments[1]
    if (arg2 is VimString && arg2.value.isEmpty()) {
      throw exExceptionMessage("E1132")
    }

    val func = arguments[1].toVimFuncref(editor, context, vimContext)
    val apply = { acc: VimDataType, value: VimDataType ->
      val args = listOf(SimpleExpression(acc), SimpleExpression(value))
      func.execute(args, range = null, editor, context, vimContext)
    }

    val initial = arguments.getOrNull(2)
    return if (initial == null) {
      iterable.reduce(apply)
    } else {
      iterable.fold(initial, apply)
    }
  }
}
