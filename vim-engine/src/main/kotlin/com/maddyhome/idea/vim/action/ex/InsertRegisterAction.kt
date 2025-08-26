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
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.state.KeyHandlerState
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

@CommandOrMotion(keys = ["<C-R>"], modes = [Mode.CMD_LINE])
class InsertRegisterAction : CommandLineActionHandler() {
  override val argumentType = Argument.Type.CHARACTER

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
    val registerName = (argument as? Argument.Character)?.character ?: return false
    if (!(injector.registerGroup.isValid(registerName))) return false

    val register = injector.registerGroup.getRegister(editor, context, registerName) ?: return false

    // If we have any non-text characters, replay them through the key handler. If it's all just plain text, insert it
    // as text. Since we're not allowed to do mapping, replaying text keystrokes should be the same as inserting text.
    if (register.keys.any { it.keyChar == KeyEvent.CHAR_UNDEFINED }) {
      register.keys.forEach { key ->
        val keyHandler = KeyHandler.getInstance()
        if (shouldHandleLiterally(key)) {
          // Reuse existing mechanisms to insert a control character literally by passing <C-V> first
          injector.parser.parseKeys("<C-V>").forEach {
            keyHandler.handleKey(editor, it, context, keyHandler.keyHandlerState)
          }
        }
        keyHandler.handleKey(editor, key, context, allowKeyMappings = false, keyHandler.keyHandlerState)
      }
    }
    else {
      commandLine.insertText(commandLine.caret.offset, register.text)
    }
    return true
  }

  private fun shouldHandleLiterally(key: KeyStroke): Boolean {
    // <C-C>, <Esc> and <CR> are inserted literally. This includes their synonyms
    if (key.keyCode == KeyEvent.VK_ENTER || key.keyCode == KeyEvent.VK_ESCAPE) return true
    if (key.keyChar == KeyEvent.CHAR_UNDEFINED && key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) {
      return key.keyCode == KeyEvent.VK_C
        || key.keyCode == KeyEvent.VK_J
        || key.keyCode == KeyEvent.VK_M
        || key.keyCode == KeyEvent.VK_OPEN_BRACKET
    }
    return false
  }

  override fun execute(commandLine: VimCommandLine): Boolean {
    error("Should not be called. Use execute(commandLine, editor, context, argument: Argument?)")
  }
}
