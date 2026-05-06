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
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.functions.UnaryFunctionHandler

@VimscriptFunction("histnr")
internal class HistNrFunctionHandler : UnaryFunctionHandler<VimInt>() {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimInt {
    val history = arguments.getString(0).value

    val historyType = VimHistory.Type.getTypeByString(history)
      ?: injector.commandLine.getActiveCommandLine()?.historyType
      ?: return VimInt.MINUS_ONE

    val entries = injector.historyGroup.getEntries(historyType, -1, 0)
    val item = entries.lastOrNull()
    return item?.number?.asVimInt() ?: VimInt.MINUS_ONE
  }
}
