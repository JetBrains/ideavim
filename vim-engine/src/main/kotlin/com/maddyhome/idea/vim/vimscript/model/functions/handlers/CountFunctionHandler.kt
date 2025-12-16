/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction(name = "count")
internal class CountFunctionHandler : BuiltinFunctionHandler<VimInt>(minArity = 2, maxArity = 4) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimInt {
    val comp = arguments[0]
    val expr = arguments[1]
    val ic = arguments.getNumberOrNull(2)?.booleanValue ?: false
    val start = arguments.getNumberOrNull(3)?.value ?: 0

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
        items.count { item -> item.valueEquals(expr, ic) }.asVimInt()
      }

      is VimDictionary -> comp.dictionary.values.count { item -> item.valueEquals(expr, ic) }.asVimInt()
      else -> VimInt.ZERO
    }
  }
}
