/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers.variousFunctions

import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction(name = "mode")
internal class ModeFunctionHandler : BuiltinFunctionHandler<VimString>(minArity = 0, maxArity = 1) {
  override fun doFunction(
    arguments: Arguments,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimString {
    return VimString(modeString(editor.mode))
  }

  private fun modeString(mode: Mode): String = when (mode) {
    is Mode.NORMAL -> "n"
    is Mode.OP_PENDING -> "no"
    is Mode.INSERT -> "i"
    is Mode.REPLACE -> "R"
    is Mode.VISUAL -> when (mode.selectionType) {
      SelectionType.CHARACTER_WISE -> "v"
      SelectionType.LINE_WISE -> "V"
      SelectionType.BLOCK_WISE -> CTRL_V
    }

    is Mode.SELECT -> when (mode.selectionType) {
      SelectionType.CHARACTER_WISE -> "s"
      SelectionType.LINE_WISE -> "S"
      SelectionType.BLOCK_WISE -> CTRL_S
    }

    is Mode.CMD_LINE -> "c"
  }

  companion object {
    private const val CTRL_V = ""
    private const val CTRL_S = ""
  }
}
