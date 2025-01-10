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
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

@CommandOrMotion(keys = ["<C-[>", "<C-C>", "<Esc>"], modes = [Mode.INSERT])
class InsertExitModeAction : VimActionHandler.SingleExecution() {
  // Note that hitting Escape can insert text when exiting insert mode after visual block mode.
  // If the editor is read-only, we'll get a "This view is read-only" tooltip. However, we should only enter insert
  // mode if both editor and document are writable.
  override val type: Command.Type = Command.Type.INSERT

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    editor.exitInsertMode(context)
    return true
  }
}
