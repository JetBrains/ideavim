/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.insert

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.api.VimMarkService
import com.maddyhome.idea.vim.mark.VimMarkConstants.MARK_CHANGE_END
import com.maddyhome.idea.vim.state.mode.Mode as VimMode

@CommandOrMotion(keys = ["<C-[>", "<C-C>", "<Esc>"], modes = [Mode.INSERT])
class InsertExitModeAction : VimActionHandler.SingleExecution() {
  // Note: ESC should not require write access itself; any write is gated in processEscape.
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    editor.exitInsertMode(context)
    return true
  }
}
