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
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.state.KeyHandlerState
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

@CommandOrMotion(keys = ["<C-R>"], modes = [Mode.CMD_LINE])
class InsertRegisterAction : CommandLineActionHandler() {
  override val argumentType: Argument.Type = Argument.Type.CHARACTER
  override val type: Command.Type = Command.Type.OTHER_WRITABLE

  override fun onStartWaitingForArgument(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    val cmdLine = injector.commandLine.getActiveCommandLine() ?: return
    cmdLine.setPromptCharacter('"')
  }

  override fun execute(
    commandLine: VimCommandLine,
    editor: VimEditor,
    context: ExecutionContext,
    argument: Argument?
  ): Boolean {
    val caretOffset = commandLine.caret.offset

    val argument = argument as? Argument.Character ?: return false
    val keyStroke = KeyStroke.getKeyStroke(argument.character)
    val pasteContent = if ((keyStroke.modifiers and KeyEvent.CTRL_DOWN_MASK) == 0) {
      injector.registerGroup.getRegister(editor, context, keyStroke.keyChar)?.text
    } else {
      throw ExException("Not yet implemented")
    } ?: return false
    commandLine.insertText(caretOffset, pasteContent)
    commandLine.caret.offset = caretOffset + pasteContent.length
    return true
  }

  override fun execute(commandLine: VimCommandLine): Boolean {
    TODO("Not yet implemented")
  }
}
