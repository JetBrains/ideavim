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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimString
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction("histget")
internal class HistGetFunctionHandler : BuiltinFunctionHandler<VimString>(minArity = 1, maxArity = 2) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimString {
    val history = arguments.getString(0).value
    val index = arguments.getNumberOrNull(1)?.value ?: -1

    if (index == 0) {
      return VimString.EMPTY
    }

    val historyType = VimHistory.Type.getTypeByString(history)
      ?: injector.commandLine.getActiveCommandLine()?.historyType
      ?: return VimString.EMPTY

    val entries = injector.historyGroup.getEntries(historyType, index, 0)
    return if (entries.size == 1) entries[0].entry.asVimString() else VimString.EMPTY
  }
}
