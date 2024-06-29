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
import com.maddyhome.idea.vim.action.change.LazyVimCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.common.CurrentCommandState
import com.maddyhome.idea.vim.common.argumentCaptured
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.key.CommandNode
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.key.Node
import com.maddyhome.idea.vim.state.KeyHandlerState
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.returnTo
import javax.swing.KeyStroke

class CommandConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<CommandConsumer>()
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    mappingCompleted: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
    shouldRecord: KeyHandler.MutableBoolean,
  ): Boolean {
    logger.trace { "Entered CommandConsumer" }
    val commandBuilder = keyProcessResultBuilder.state.commandBuilder
    // Ask the key/action tree if this is an appropriate key at this point in the command and if so,
    // return the node matching this keystroke
    logger.trace("Getting the node for the current mode")
    logger.trace("command builder - $commandBuilder")
    val node: Node<LazyVimCommand>? = mapOpCommand(key, commandBuilder.getChildNode(key), editor.mode, keyProcessResultBuilder.state)
    logger.trace("node: $node")

    when (node) {
      is CommandNode<LazyVimCommand> -> {
        logger.trace("Node is a command node")
        handleCommandNode(node, keyProcessResultBuilder)
        keyProcessResultBuilder.addExecutionStep { lambdaKeyState, _, _ -> lambdaKeyState.commandBuilder.addKey(key) }
        return true
      }

      is CommandPartNode<LazyVimCommand> -> {
        logger.trace("Node is a command part node")
        commandBuilder.setCurrentCommandPartNode(node)
        commandBuilder.addKey(key)
        return true
      }

      else -> {
        return false
      }
    }
  }

  /**
   * See the description for [com.maddyhome.idea.vim.command.DuplicableOperatorAction]
   */
  private fun mapOpCommand(
    key: KeyStroke,
    node: Node<LazyVimCommand>?,
    mode: Mode,
    keyState: KeyHandlerState,
  ): Node<LazyVimCommand>? {
    logger.trace("entered mapOpCommand. key = ${ injector.parser.toKeyNotation(key) }, node = $node, mode = $mode")
    return if (KeyHandler.getInstance().isDuplicateOperatorKeyStroke(key, mode, keyState)) {
      logger.trace("it is a duplicate operator key stroke")
      keyState.commandBuilder.getChildNode(KeyStroke.getKeyStroke('_'))
    } else {
      node
    }
  }

  private fun handleCommandNode(node: CommandNode<LazyVimCommand>, processBuilder: KeyProcessResult.KeyProcessResultBuilder) {
    logger.trace("Handle command node")
    // The user entered a valid command. Create the command and add it to the stack.
    val action = node.actionHolder.instance
    val keyState = processBuilder.state

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
    commandBuilder.pushCommandPart(action)
    if (!checkArgumentCompatibility(expectedArgumentType, action)) {
      logger.trace("Return from command node handling")
      processBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, _ ->
        KeyHandler.getInstance().setBadCommand(lambdaEditor, lambdaKeyState)
      }
      return
    }
    if (action.argumentType == null || stopMacroRecord(node)) {
      logger.trace("Set command state to READY")
      commandBuilder.commandState = CurrentCommandState.READY
    } else {
      processBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, lambdaContext ->
        logger.trace("Set waiting for the argument")
        val argumentType = action.argumentType
        startWaitingForArgument(lambdaEditor, lambdaContext, action, argumentType!!, lambdaKeyState, injector.vimState)
        lambdaKeyState.partialReset(lambdaEditor.mode)
      }
    }

    processBuilder.addExecutionStep { _, lambdaEditor, _ ->
      if (action.flags.contains(CommandFlags.FLAG_END_EX)) {
        logger.trace("Processing ex_string")
        val commandLine = injector.commandLine.getActiveCommandLine()!!
        val label = commandLine.label
        val text = commandLine.actualText
        commandLine.deactivate(refocusOwningEditor = true, resetCaret = true)

        commandBuilder.completeCommandPart(Argument(label[0], text))
        lambdaEditor.mode = lambdaEditor.mode.returnTo()
      }
    }
  }

  private fun stopMacroRecord(node: CommandNode<LazyVimCommand>): Boolean {
    return injector.registerGroup.isRecording && node.actionHolder.instance.id == "VimToggleRecordingAction"
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
    when (argument) {
      Argument.Type.MOTION -> {
        if (editorState.isDotRepeatInProgress && argumentCaptured != null) {
          commandBuilder.completeCommandPart(argumentCaptured!!)
        }
        editor.mode = Mode.OP_PENDING(editorState.mode.returnTo)
      }

      Argument.Type.DIGRAPH -> // Command actions represent the completion of a command. Showcmd relies on this - if the action represents a
        // part of a command, the showcmd output is reset part way through. This means we need to special case entering
        // digraph/literal input mode. We have an action that takes a digraph as an argument, and pushes it back through
        // the key handler when it's complete.

        // TODO
//        if (action is InsertCompletedDigraphAction) {
        if (action.id == "VimInsertCompletedDigraphAction") {
          keyState.digraphSequence.startDigraphSequence()
          KeyHandler.getInstance().setPromptCharacterEx('?')
        } else if (action.id == "VimInsertCompletedLiteralAction") {
          keyState.digraphSequence.startLiteralSequence()
          KeyHandler.getInstance().setPromptCharacterEx('^')
        }

      else -> Unit
    }

    // Another special case. Force a mode change to update the caret shape
    // This was a typed solution
    // if (action is ChangeCharacterAction || action is ChangeVisualCharacterAction)
    if (action.id == "VimChangeCharacterAction" || action.id == "VimChangeVisualCharacterAction") {
      editor.isReplaceCharacter = true
    }
    action.onStartWaitingForArgument(editor, context)
  }

  private fun checkArgumentCompatibility(
    expectedArgumentType: Argument.Type?,
    action: EditorActionHandlerBase,
  ): Boolean {
    return !(expectedArgumentType === Argument.Type.MOTION && action.type !== Command.Type.MOTION)
  }
}
