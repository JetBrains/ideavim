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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimList
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler

@VimscriptFunction(name = "items")
internal class ItemsFunctionHandler : UnaryFunctionHandler<VimList>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimList {
    val arg = arguments[0]
    return when (arg) {
      is VimList -> VimList(mutableListOf()).also {
        arg.values.forEachIndexed { index, value ->
          it.values.add(VimList(mutableListOf(VimInt(index), value)))
        }
      }

      is VimDictionary -> VimList(mutableListOf()).also {
        arg.dictionary.forEach { (key, value) ->
          it.values.add(VimList(mutableListOf(key, value)))
        }
      }

      is VimString -> {
        VimList(mutableListOf()).also {
          arg.value.forEachIndexed { index, ch ->
            it.values.add(VimList(mutableListOf(VimInt(index), VimString(ch.toString()))))
          }
        }
      }

      else -> throw exExceptionMessage("E1225", 1)
    }
  }
}
