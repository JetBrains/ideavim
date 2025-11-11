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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandler

@VimscriptFunction(name = "count")
internal class CountFunctionHandler : FunctionHandler() {
  override val minimumNumberOfArguments = 2
  override val maximumNumberOfArguments = 4

  override fun doFunction(
    argumentValues: List<Expression>,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val comp = argumentValues[0].evaluate(editor, context, vimContext)
    val expr = argumentValues[1].evaluate(editor, context, vimContext)
    val ic = argumentValues.getOrNull(2)?.evaluate(editor, context, vimContext)?.toVimNumber()?.value != 0
    val start = argumentValues.getOrNull(3)?.evaluate(editor, context, vimContext)?.toVimNumber()?.value ?: 0

    return when (comp) {
      is VimString -> {
        // Count non-overlapping occurrences in string
        val text = comp.value
        val pattern = expr.toVimString().value
        if (pattern.isEmpty()) {
          return VimInt(0)
        }
        var count = 0
        var index = 0
        while (index <= text.length - pattern.length) {
          val substring = text.substring(index, index + pattern.length)
          val matches = if (ic) {
            substring.equals(pattern, ignoreCase = true)
          } else {
            substring == pattern
          }
          if (matches) {
            count++
            index += pattern.length // Non-overlapping
          } else {
            index++
          }
        }
        VimInt(count)
      }
      is VimList -> {
        val items = if (start > 0 && start < comp.values.size) {
          comp.values.subList(start, comp.values.size)
        } else {
          comp.values
        }
        val count = items.count { item ->
          compareValues(item, expr, ic)
        }
        VimInt(count)
      }
      is VimDictionary -> {
        val count = comp.dictionary.values.count { item ->
          compareValues(item, expr, ic)
        }
        VimInt(count)
      }
      else -> VimInt(0)
    }
  }

  private fun compareValues(item: VimDataType, expr: VimDataType, ignoreCase: Boolean): Boolean {
    return if (ignoreCase && item is VimString && expr is VimString) {
      item.value.equals(expr.value, ignoreCase = true)
    } else {
      // Direct comparison - no automatic conversion
      when {
        item is VimInt && expr is VimInt -> item.value == expr.value
        item is VimString && expr is VimString -> item.value == expr.value
        else -> false
      }
    }
  }
}
