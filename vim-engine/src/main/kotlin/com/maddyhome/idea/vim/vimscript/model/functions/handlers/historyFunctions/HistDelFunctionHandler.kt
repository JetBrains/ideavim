/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.historyFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.history.VimHistory
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction("histdel")
internal class HistDelFunctionHandler : BuiltinFunctionHandler<VimDataType>(minArity = 1, maxArity = 2) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    val history = arguments.getString(0).value
    val item = arguments.getOrNull(1)

    val historyType = VimHistory.Type.getTypeByString(history)
      ?: injector.commandLine.getActiveCommandLine()?.historyType
      ?: return VimInt.ZERO

    return when (item) {
      is VimInt -> {
        if (item.value == 0) VimInt.ZERO else injector.historyGroup.removeEntry(historyType, item.value).asVimInt()
      }

      is VimString -> injector.historyGroup.removeEntries(historyType, item.value).asVimInt()

      else -> {
        injector.historyGroup.clearHistory(historyType)
        VimInt.ONE
      }
    }
  }
}
