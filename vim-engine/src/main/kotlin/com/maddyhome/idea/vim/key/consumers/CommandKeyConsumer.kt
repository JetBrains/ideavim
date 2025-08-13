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
import com.maddyhome.idea.vim.command.CommandBuilder
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

/**
 * Tries to consume a key that is the start of a new command, or continuation of an in-progress command
 *
 * This consumer will process all input. It delegates to [CommandBuilder] and if the key is processed as part of a new
 * or ongoing command, the key is consumed. If the command maps to a complete action, it is added to the
 * [CommandBuilder], and either waits for an argument, or is invoked when all key consumers have finished.
 *
 * The key consumer is responsible for updating the [CommandBuilder] if the discovered action needs to start or leave
 * Command-line mode, although it does not change the mode directly. This is similar to supporting a "nested" command
 * builder so that a Normal mode command can include a Command-line command, e.g. `d/foo`.
 *
 * Escape and cancel keys can match as completed commands, in which case the command handler will be invoked once all
 * key consumers are done. If a command is not yet completed, the escape and cancel keys will fail to map to a known
 * built-in key shortcut and refuse to process the key.
 *
 * In practice, this means we can implement escape and cancel command handlers for the happy paths, but other key
 * consumers need to handle escape and cancel for all other scenarios. E.g., when selecting the new current register.
 *
 * TODO: This is currently handled by [EditorResetConsumer], but this doesn't do anything for non-Normal modes
 * Escape in Op-pending falls through to onUnknownKey, which might be the best implementation
 */
internal class CommandKeyConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<CommandKeyConsumer>()
  }

  override fun isApplicable(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    // A key is a command key if there are no expected arguments (we're at the start of a command, or a multi-key
    // command is still in progress), or if the expected argument is a motion, which is a command.
    // TODO: Get rid of the check for digraphs
    // In Command-line mode, <C-C> is supposed to cancel the command line, even if we're in the middle of another
    // command, either as a DIGRAPH or CHARACTER argument, or while entering a multi-key command.
    // Up to now, we only support this while entering a DIGRAPH, by having a special case check in DigraphConsumer, and
    // dropping through to CommandKeyConsumer to match and call LeaveCommandLineAction. (This shouldn't actually work,
    // because we're matching a command when we're not in a state to match a command - we're still waiting for the
    // argument to the last command. Historically, this check wasn't made.)
    // We don't support cancelling command line while entering a CHARACTER argument, and the only multi-key command
    // we've supported to date is c_CTRL-\-CTRL-N, which actually appears to be broken; we don't close the command line.
    // Because we need to support <C-C> at arbitrary points, and not just where a new command is expected, we should
    // introduce a new key consumer to handle this, at which point we can clean up the digraph logic.
    // Note that in Vim, <Esc> will also exit command line, if 'x' is in the 'cpoptions' option, which we don't support.
    val commandBuilder = keyProcessResultBuilder.state.commandBuilder
    return commandBuilder.expectedArgumentType == null || commandBuilder.expectedArgumentType == Argument.Type.MOTION
      || commandBuilder.expectedArgumentType == Argument.Type.DIGRAPH
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
