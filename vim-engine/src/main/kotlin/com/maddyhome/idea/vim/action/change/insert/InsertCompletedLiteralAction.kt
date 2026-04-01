/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change.insert

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.isCommandLineActionChar
import com.maddyhome.idea.vim.state.KeyHandlerState
import javax.swing.KeyStroke

/**
 * Insert mode: insert a literal character via `<C-V>` / `<C-Q>`
 *
 * The converted literal character is re-injected through the key handler so that it is processed as typed input in
 * Insert mode (handled by the change group).
 */
@CommandOrMotion(keys = ["<C-V>", "<C-Q>"], modes = [Mode.INSERT])
class InsertCompletedLiteralAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  // TODO: This should really just be CHARACTER
  // The DIGRAPH type indicates that the key handler can start the digraph state machine, but we've already started it.
  // We're waiting for it to complete and give us a CHARACTER
  override val argumentType: Argument.Type = Argument.Type.DIGRAPH

  override fun onStartWaitingForArgument(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    val result = keyState.digraphSequence.startLiteralSequence()
    KeyHandler.getInstance().setPromptCharacterEx(result.promptCharacter)
  }

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val argument = cmd.argument as? Argument.Character ?: return false
    val keyStroke = KeyStroke.getKeyStroke(argument.character)
    val keyHandler = KeyHandler.getInstance()
    keyHandler.handleKey(editor, keyStroke, context, keyHandler.keyHandlerState)
    return true
  }
}

/**
 * Command-line mode: insert a literal character via `<C-V>` / `<C-Q>`
 *
 * Control characters like Escape or Enter are inserted directly into the command line to avoid being matched as
 * commands (e.g., LeaveCommandLineAction). Other characters use [VimCommandLine.handleKey] so that overwrite mode
 * is handled correctly.
 */
@CommandOrMotion(keys = ["<C-V>", "<C-Q>"], modes = [Mode.CMD_LINE])
class CmdLineCompletedLiteralAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT
  override val argumentType: Argument.Type = Argument.Type.DIGRAPH

  override fun onStartWaitingForArgument(editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    val result = keyState.digraphSequence.startLiteralSequence()
    KeyHandler.getInstance().setPromptCharacterEx(result.promptCharacter)
  }

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val argument = cmd.argument as? Argument.Character ?: return false
    val commandLine = injector.commandLine.getActiveCommandLine() ?: return false
    val ch = argument.character
    if (ch.isCommandLineActionChar()) {
      // Insert directly to avoid these being matched as commands by the key handler
      commandLine.insertText(commandLine.caret.offset, ch.toString())
    } else {
      commandLine.handleKey(KeyStroke.getKeyStroke(ch))
    }
    return true
  }
}
