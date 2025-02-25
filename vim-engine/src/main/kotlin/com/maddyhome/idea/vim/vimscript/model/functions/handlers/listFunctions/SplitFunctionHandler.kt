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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

@VimscriptFunction(name = "split")
internal class SplitFunctionHandler : FunctionHandler() {
  override val minimumNumberOfArguments: Int = 1
  override val maximumNumberOfArguments: Int = 3

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val text = argumentValues[0].evaluate(editor, context, vimContext).asString()
    val delimiter = argumentValues.getOrNull(1)?.evaluate(editor, context, vimContext)?.asString() ?: "\\s\\+"
    val keepEmpty = argumentValues.getOrNull(2)?.evaluate(editor, context, vimContext)?.toVimNumber()?.booleanValue ?: false

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
