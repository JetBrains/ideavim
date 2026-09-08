/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.expressions

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimFuncref
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList

data class SublistExpression(val from: Expression?, val to: Expression?, val expression: Expression)
  : LValueExpression() {

  override fun evaluate(editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimDataType {
    val expressionValue = expression.evaluate(editor, context, vimContext)
    val start = from?.evaluate(editor, context, vimContext)?.toVimNumber()?.value ?: 0
    val endInclusive = to?.evaluate(editor, context, vimContext)?.toVimNumber()?.value ?: -1

    return when (expressionValue) {
      is VimDictionary -> throw exExceptionMessage("E719")
      is VimFuncref -> throw exExceptionMessage("E695")
      is VimList -> expressionValue.slice(start, toEndExclusive(endInclusive, expressionValue.size))
      else -> {
        val string = expressionValue.toVimString()
        string.substring(start, toEndExclusive(endInclusive, string.value.length))
      }
    }
  }

  override fun isStronglyTyped() = false

  override fun assign(
    value: VimDataType,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
    assignmentTextForErrors: String,
  ) {
    val listValue = expression.evaluate(editor, context, vimContext)
    when (listValue) {
      is VimList -> {
        val newList = value as? VimList ?: throw exExceptionMessage("E709")
        val total = listValue.values.size
        val from = from?.evaluate(editor, context, vimContext)?.toVimNumber()?.value ?: 0
        val to = to?.evaluate(editor, context, vimContext)?.toVimNumber()?.value ?: (total - 1)

        // Negative numbers are offsets from the end. If our start index is below 0, treat it as 0
        val start = (if (from < 0) from + total else from).coerceAtLeast(0)
        val end = if (to < 0) to + total else to

        if (start >= listValue.values.size) {
          throw exExceptionMessage("E684", from)
        } else if (end < 0) {
          throw exExceptionMessage("E684", to)
        }

        if (newList.values.size < end - start + 1) {
          // I.e., the _new_ list value does not have enough items
          throw exExceptionMessage("E711")
        } else if (newList.values.size > end - start + 1 && end < listValue.values.size - 1) {
          // Remember that if the new list "overflows" the sublist range, we'll add new items to the original list
          // I.e., the _new_ list has more items than the target sublist range
          throw exExceptionMessage("E710")
        }

        for (i in 0 until newList.values.size) {
          val item = newList.values[i]
          if (i + start < listValue.values.size) {
            listValue.values[i + start] = item
          } else {
            listValue.values.add(item)
          }
        }
      }

      is VimBlob -> TODO()
      is VimDictionary -> throw exExceptionMessage("E719")
      else -> throw exExceptionMessage("E689", listValue.typeName, assignmentTextForErrors)
    }
  }

  /**
   * Converts Vim's inclusive sublist end index into the resolved exclusive end index used by [VimList.slice]
   *
   * A negative index counts from the end of the collection, and is resolved before being made exclusive, so that the
   * default end index of `-1` becomes an exclusive end index of [size] rather than zero, which would be empty.
   */
  private fun toEndExclusive(endInclusive: Int, size: Int) =
    (if (endInclusive < 0) endInclusive + size else endInclusive) + 1
}
