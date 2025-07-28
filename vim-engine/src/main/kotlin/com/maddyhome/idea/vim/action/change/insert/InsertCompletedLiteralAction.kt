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
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.state.KeyHandlerState
import com.maddyhome.idea.vim.key.VimKeyStroke

@CommandOrMotion(keys = ["<C-V>", "<C-Q>"], modes = [Mode.INSERT, Mode.CMD_LINE])
class InsertCompletedLiteralAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.INSERT

  // TODO: This should really just be CHARACTER
  // The DIGRAPH type indicates that the key handler can start the digraph state machine, but we've already started it.
  // We're waiting for it to complete and give us a CHARACTER
  override val argumentType: Argument.Type = Argument.Type.DIGRAPH

  /**
   * Perform additional initialisation when starting to wait for an argument
   *
   * IdeaVim has two ways of handling digraphs/literals. Actions such as `r` or `f` can accept a digraph, which really
   * means it accepts a character, but the user can use `<C-K>`/`<C-V>` to type a digraph or literal and convert it into
   * a character. Unfortunately, there is no mode that can be used to register an "insert digraph/literal" action for
   * these keys while replace or find is active. So the key handler hard codes these keys and will check for them when
   * an action expects a digraph (and like Vim, these keys cannot be mapped). Once the state machine has matched a
   * character, the expected argument is reset to [Argument.Type.CHARACTER] and the character is passed through the key
   * handler again, potentially mapped, and then attached as an argument to the current command, which is now complete
   * and executed.
   *
   * In Insert and Command-line mode, the `<C-K>` and `<C-V>` keys are actions that will wait for a character argument,
   * and then insert it. Commands are only executed once complete, so we use [onStartWaitingForArgument] to start the
   * digraph state machine. This also gives us a repeatable command and captures the keys for `'showcmd'`.
   */
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
    // The converted literal character has been captured as an argument, push it back through key handler
    val argument = cmd.argument as? Argument.Character ?: return false
    val keyStroke = VimKeyStroke.getKeyStroke(argument.character)
    val keyHandler = KeyHandler.getInstance()
    keyHandler.handleKey(editor, keyStroke, context, keyHandler.keyHandlerState)
    return true
  }
}
