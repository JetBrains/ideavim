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
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimBlob
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.functions.FunctionHandlerBase

@VimscriptFunction(name = "remove")
internal class RemoveFunctionHandler : FunctionHandlerBase<VimDataType>(minArity = 1, maxArity = 3) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val container = arguments[0]

    when (container) {
      is VimList -> {
        val idx = arguments.getNumber(1).value
        val index = if (idx < 0) idx + container.values.size else idx
        if (index < 0 || index >= container.values.size) {
          throw exExceptionMessage("E684", idx)
        }

        val end = arguments.getNumberOrNull(2)?.value ?: return container.values.removeAt(index)

        val endIndex = if (end < 0) end + container.values.size else end
        if (endIndex < 0 || endIndex > container.values.size) {
          throw exExceptionMessage("E684", end)
        }

        if (endIndex < index) {
          throw exExceptionMessage("E16")
        }

        // Note that remove() doesn't check lockvar!
        val items = mutableListOf<VimDataType>()
        repeat(endIndex - index + 1) {
          items.add(container.values.removeAt(index))
        }
        return VimList(items)
      }

      is VimDictionary -> {
        if (arguments.size == 3) {
          throw exExceptionMessage("E118", "remove()")
        }
        val key = arguments.getString(1)
        if (!container.dictionary.containsKey(key)) {
          throw exExceptionMessage("E716", key.toOutputString())
        }
        return container.dictionary.remove(key)!!
      }

      is VimBlob -> TODO()
      else -> throw exExceptionMessage("E896", "remove()")
    }
  }
}
