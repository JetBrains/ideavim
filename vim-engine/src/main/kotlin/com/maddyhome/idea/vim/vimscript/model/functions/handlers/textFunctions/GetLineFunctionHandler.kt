/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.textFunctions

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
import com.maddyhome.idea.vim.vimscript.model.functions.handlers.cursorFunctions.variableToPosition

@VimscriptFunction(name = "getline")
internal class GetLineFunctionHandler : FunctionHandler() {
  override val minimumNumberOfArguments = 1
  override val maximumNumberOfArguments = 2

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    fun exprToLine(expr: Expression): Int? {
      val value = expr.evaluate(editor, context, vimContext)
      return when (value) {
        is VimInt -> value.value
        is VimString -> {
          val s = value.value
          if (s.isNotEmpty() && s[0].isDigit()) {
            VimInt.parseNumber(s, allowTrailingCharacters = true)?.value
          } else {
            variableToPosition(editor, value, true)?.first?.value
          }
        }
        else -> variableToPosition(editor, value, true)?.first?.value
      }
    }

    val startLine1 = exprToLine(argumentValues[0]) ?: return VimString.EMPTY
    val lineCount = editor.lineCount()

    if (argumentValues.size == 1) {
      // Single line. Return empty string if out of range
      if (startLine1 !in 1..lineCount) return VimString.EMPTY
      val text = editor.getLineText(startLine1 - 1)
      return VimString(text)
    }

    // Range: return list of lines
    val endLine1 = exprToLine(argumentValues[1]) ?: return VimList(mutableListOf())

    // Clamp to valid buffer range
    val start = startLine1.coerceAtLeast(1)
    val end = endLine1.coerceAtMost(lineCount)
    if (end < start || start > lineCount) return VimList(mutableListOf())

    val values = mutableListOf<VimDataType>()
    for (lnum in start..end) {
      values.add(VimString(editor.getLineText(lnum - 1)))
    }
    return VimList(values)
  }
}
