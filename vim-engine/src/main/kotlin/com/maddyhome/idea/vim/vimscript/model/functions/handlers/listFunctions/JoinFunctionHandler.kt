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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

@VimscriptFunction(name = "join")
internal class JoinFunctionHandler : FunctionHandler() {
  override val minimumNumberOfArguments: Int = 1
  override val maximumNumberOfArguments: Int = 2

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val list = argumentValues[0].evaluate(editor, context, vimContext)
    if (list !is VimList) {
      throw exExceptionMessage("E1211", "1") // E1211: List required for argument 1
    }
    val separator = argumentValues.getOrNull(1)?.evaluate(editor, context, vimContext)?.asString() ?: " "
    return VimString(list.values.joinToString(separator) {
      // According to the docs, String is used as-is, while List and Dictionary (and presumably the other datatypes) are
      // converted in the same way as Vim's string() function, which has the same results as toEchoString()
      when (it) {
        is VimString -> it.value
        else -> it.toOutputString()
      }
    })
  }
}
