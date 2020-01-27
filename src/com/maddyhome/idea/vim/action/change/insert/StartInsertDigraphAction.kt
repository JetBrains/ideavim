package com.maddyhome.idea.vim.action.change.insert

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.VimActionHandler

class StartInsertDigraphAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    KeyHandler.getInstance().startDigraphSequence(editor)
    return true
  }
}
