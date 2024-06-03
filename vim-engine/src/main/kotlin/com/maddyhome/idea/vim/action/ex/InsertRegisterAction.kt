/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.handler.VimActionHandler
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

@CommandOrMotion(keys = ["<C-R>"], modes = [Mode.CMD_LINE])
public class InsertRegisterAction: VimActionHandler.SingleExecution() {
  override val argumentType: Argument.Type = Argument.Type.CHARACTER
  override val type: Command.Type = Command.Type.OTHER_WRITABLE

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    val cmdLine = injector.commandLine.getActiveCommandLine() ?: return false
    val caretOffset = cmdLine.caret.offset

    val keyStroke = KeyStroke.getKeyStroke(cmd.argument!!.character)
    val pasteContent = if ((keyStroke.modifiers and KeyEvent.CTRL_DOWN_MASK) == 0)  {
      injector.registerGroup.getRegister(keyStroke.keyChar)?.text
    } else {
      throw ExException("Not yet implemented")
    } ?: return false
    cmdLine.setText(StringBuilder(cmdLine.actualText).insert(caretOffset, pasteContent).toString())
    cmdLine.caret.offset = caretOffset + pasteContent.length
    return true
  }
}