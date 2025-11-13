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
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

@VimscriptFunction(name = "index")
internal class IndexFunctionHandler : FunctionHandler() {
  override val minimumNumberOfArguments = 2
  override val maximumNumberOfArguments = 4

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val obj = argumentValues[0].evaluate(editor, context, vimContext)
    val expr = argumentValues[1].evaluate(editor, context, vimContext)
    val start = argumentValues.getOrNull(2)?.evaluate(editor, context, vimContext)?.toVimNumber()?.value ?: 0
    val ic = argumentValues.getOrNull(3)?.evaluate(editor, context, vimContext)?.toVimNumber()?.value != 0

    if (obj !is VimList) {
      return VimInt(-1)
    }

    val startIndex = if (start < 0) {
      (obj.values.size + start).coerceAtLeast(0)
    } else {
      start.coerceAtMost(obj.values.size)
    }

    for (i in startIndex until obj.values.size) {
      val item = obj.values[i]
      if (compareValues(item, expr, ic)) {
        return VimInt(i)
      }
    }

    return VimInt(-1)
  }

  private fun compareValues(item: VimDataType, expr: VimDataType, ignoreCase: Boolean): Boolean {
    return if (ignoreCase && item is VimString && expr is VimString) {
      item.value.equals(expr.value, ignoreCase = true)
    } else {
      // Direct comparison - no automatic conversion
      // String "4" is different from Number 4
      when {
        item is VimInt && expr is VimInt -> item.value == expr.value
        item is VimString && expr is VimString -> item.value == expr.value
        item.javaClass == expr.javaClass -> item == expr
        else -> false
      }
    }
  }
}
