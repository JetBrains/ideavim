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
import com.maddyhome.idea.vim.extension.VimExtensionFacade.inputKeyStroke
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.inVisualMode
import java.awt.event.KeyEvent

/**
 * Asks for a register, and runs its macro at each cursor. The regions become cursors first.
 */
internal class RunMacroHandler : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val key = inputKeyStroke(editor.ij)
    if (key.keyCode == KeyEvent.VK_ESCAPE || key.keyChar == KeyEvent.CHAR_UNDEFINED) return

    if (editor.inVisualMode) {
      collapseRegionsToCursors(editor)
    }
    injector.vimscriptExecutor.execute("normal! @${key.keyChar}", editor, context, skipHistory = true)
  }
}
