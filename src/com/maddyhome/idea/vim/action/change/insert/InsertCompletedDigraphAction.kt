package com.maddyhome.idea.vim.action.change.insert

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.handler.VimActionHandler
import javax.swing.KeyStroke

class InsertCompletedDigraphAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT
  override val argumentType: Argument.Type? = Argument.Type.DIGRAPH

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    // The converted digraph character has been captured as an argument, push it back through key handler
    val keyStroke = KeyStroke.getKeyStroke(cmd.argument!!.character)
    KeyHandler.getInstance().handleKey(editor, keyStroke, context)
    return true
  }
}
