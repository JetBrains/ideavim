/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key.consumers

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.common.argumentCaptured
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.state.KeyHandlerState
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.state.mode.Mode
import javax.swing.KeyStroke

internal class CommandConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<CommandConsumer>()
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    logger.trace("Entered CommandConsumer")
    val commandBuilder = keyProcessResultBuilder.state.commandBuilder

    logger.trace { "command builder - $commandBuilder" }

    // Map duplicate operator keystrokes. E.g., given `dd`, there is no `d` motion, so the second keystroke is mapped to
    // `_`. Same with `cc` and `yy`, etc.
    val keystroke =
      if (editor.mode is Mode.OP_PENDING) commandBuilder.convertDuplicateOperatorKeyStrokeToMotion(key) else key
    logger.trace { "Original keystroke: $key, substituted keystroke: $keystroke" }

    return commandBuilder.processKey(keystroke) { handleAction(it, keyProcessResultBuilder) }
  }

  private fun handleAction(action: EditorActionHandlerBase, processBuilder: KeyProcessResult.KeyProcessResultBuilder) {
    val keyState = processBuilder.state

    logger.trace { "Handle command action: ${action.id}" }

    if (action.flags.contains(CommandFlags.FLAG_START_EX)) {
      keyState.enterCommandLine()
      injector.redrawService.redrawStatusLine()
    }
    if (action.flags.contains(CommandFlags.FLAG_END_EX)) {
      keyState.leaveCommandLine()
      injector.redrawService.redrawStatusLine()
    }

    val commandBuilder = keyState.commandBuilder
    val expectedArgumentType = commandBuilder.expectedArgumentType
    if (!checkArgumentCompatibility(expectedArgumentType, action)) {
      logger.trace("Return from command node handling")
      processBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, _ ->
        KeyHandler.getInstance().setBadCommand(lambdaEditor, lambdaKeyState)
      }
      return
    }

    commandBuilder.addAction(action)
    if (commandBuilder.isAwaitingArgument) {
      processBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, lambdaContext ->
        logger.trace("Set waiting for the argument")
        val argumentType = action.argumentType
        startWaitingForArgument(lambdaEditor, lambdaContext, action, argumentType!!, lambdaKeyState, injector.vimState)
        lambdaKeyState.partialReset(lambdaEditor.mode)
      }
    }

    processBuilder.addExecutionStep { _, _, _ ->
      if (action.flags.contains(CommandFlags.FLAG_END_EX)) {
        logger.trace("Processing ex_string")
        val commandLine = injector.commandLine.getActiveCommandLine()!!
        val label = commandLine.label
        val text = commandLine.text
        val processing = commandLine.inputProcessing
        commandLine.close(refocusOwningEditor = true, resetCaret = true)

        commandBuilder.addArgument(Argument.ExString(label[0], text, processing))
      }
    }
  }

  private fun startWaitingForArgument(
    editor: VimEditor,
    context: ExecutionContext,
    action: EditorActionHandlerBase,
    argument: Argument.Type,
    keyState: KeyHandlerState,
    editorState: VimStateMachine,
  ) {
    val commandBuilder = keyState.commandBuilder
    if (argument == Argument.Type.MOTION) {
      if (editorState.isDotRepeatInProgress && argumentCaptured != null) {
        commandBuilder.addArgument(argumentCaptured!!)
      }
      editor.mode = Mode.OP_PENDING(editorState.mode.returnTo)
    }

    action.onStartWaitingForArgument(editor, context, keyState)
  }

  private fun checkArgumentCompatibility(
    expectedArgumentType: Argument.Type?,
    action: EditorActionHandlerBase,
  ): Boolean {
    return !(expectedArgumentType === Argument.Type.MOTION && action.type !== Command.Type.MOTION)
  }
}
