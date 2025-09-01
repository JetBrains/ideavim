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
  // Note that hitting Escape can insert text when exiting insert mode after visual block mode.
  // We use OTHER_SELF_SYNCHRONIZED to handle write locks manually, ensuring ESC always works
  // even in read-only files by providing a fallback path that doesn't require write access.
  override val type: Command.Type = Command.Type.OTHER_SELF_SYNCHRONIZED

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    // Handle write locks manually since we use OTHER_SELF_SYNCHRONIZED
    // Most of the time we don't need write access, only for repeat insert operations
    try {
      editor.exitInsertMode(context)
    } catch (e: Exception) {
      // If something fails, still try to exit insert mode without write operations
      // This ensures ESC always works even in read-only files
      val markGroup = injector.markService
      markGroup.setMark(editor, VimMarkService.INSERT_EXIT_MARK)
      markGroup.setMark(editor, MARK_CHANGE_END)
      if (editor.mode is VimMode.REPLACE) {
        editor.insertMode = true
      }
      editor.mode = VimMode.NORMAL()
    }
    return true
  }
}
