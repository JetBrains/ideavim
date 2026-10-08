/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtensionFacade.inputString
import com.maddyhome.idea.vim.extension.multiplecursors.leaveVisualMode
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.inVisualMode

/**
 * Asks for a command, or repeats the last one, and runs it with `:normal`, which uses the mappings, like
 * vim-visual-multi does. The command runs at each cursor, which is at the end of a region.
 */
internal class RunNormalHandler(private val repeatLast: Boolean) : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val command = if (repeatLast) lastCommand else inputString(editor.ij, context.ij, ":normal ", null)
    if (command.isNullOrEmpty()) return
    lastCommand = command

    if (editor.inVisualMode) {
      leaveVisualMode(editor)
    }
    injector.vimscriptExecutor.execute("normal $command", editor, context, skipHistory = true)
  }

  private companion object {
    var lastCommand: String? = null
  }
}
