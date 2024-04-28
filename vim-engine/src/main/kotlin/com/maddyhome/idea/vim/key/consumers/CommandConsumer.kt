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
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.key.CommandNode
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.key.Node
import com.maddyhome.idea.vim.state.KeyHandlerState
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.ReturnableFromCmd
import com.maddyhome.idea.vim.state.mode.returnTo
import javax.swing.KeyStroke

public class CommandConsumer : KeyConsumer {
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
    val commandBuilder = keyProcessResultBuilder.state.commandBuilder
    // Ask the key/action tree if this is an appropriate key at this point in the command and if so,
    // return the node matching this keystroke
    val node: Node<LazyVimCommand>? = mapOpCommand(key, commandBuilder.getChildNode(key), editor.mode, keyProcessResultBuilder.state)
    logger.trace("Get the node for the current mode")

    when (node) {
      is CommandNode<LazyVimCommand> -> {
        logger.trace("Node is a command node")
        handleCommandNode(key, node, keyProcessResultBuilder)
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
    return if (KeyHandler.getInstance().isDuplicateOperatorKeyStroke(key, mode, keyState)) {
      keyState.commandBuilder.getChildNode(KeyStroke.getKeyStroke('_'))
    } else {
      node
    }
  }

  private fun handleCommandNode(
    key: KeyStroke,
    node: CommandNode<LazyVimCommand>,
    processBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ) {
    logger.trace("Handle command node")
    // The user entered a valid command. Create the command and add it to the stack.
    val action = node.actionHolder.instance
    val keyState = processBuilder.state
    val commandBuilder = keyState.commandBuilder
    val expectedArgumentType = commandBuilder.expectedArgumentType
    commandBuilder.pushCommandPart(action)
    if (!checkArgumentCompatibility(expectedArgumentType, action)) {
      logger.trace("Return from command node handling")
      processBuilder.addExecutionStep { lamdaKeyState, lambdaEditor, _ ->
        KeyHandler.getInstance().setBadCommand(lambdaEditor, lamdaKeyState)
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
        val editorState = lambdaEditor.vimStateMachine
        startWaitingForArgument(lambdaEditor, lambdaContext, key.keyChar, action, argumentType!!, lambdaKeyState, editorState)
        lambdaKeyState.partialReset(editorState.mode)
      }
    }

    processBuilder.addExecutionStep { _, lambdaEditor, _ ->
      // TODO In the name of God, get rid of EX_STRING, FLAG_COMPLETE_EX and all the related staff
      if (expectedArgumentType === Argument.Type.EX_STRING && action.flags.contains(CommandFlags.FLAG_COMPLETE_EX)) {
        /* The only action that implements FLAG_COMPLETE_EX is ProcessExEntryAction.
     * When pressing ':', ExEntryAction is chosen as the command. Since it expects no arguments, it is invoked and
       calls ProcessGroup#startExCommand, pushes CMD_LINE mode, and the action is popped. The ex handler will push
       the final <CR> through handleKey, which chooses ProcessExEntryAction. Because we're not expecting EX_STRING,
       this branch does NOT fire, and ProcessExEntryAction handles the ex cmd line entry.
     * When pressing '/' or '?', SearchEntry(Fwd|Rev)Action is chosen as the command. This expects an argument of
       EX_STRING, so startWaitingForArgument calls ProcessGroup#startSearchCommand. The ex handler pushes the final
       <CR> through handleKey, which chooses ProcessExEntryAction, and we hit this branch. We don't invoke
       ProcessExEntryAction, but pop it, set the search text as an argument on SearchEntry(Fwd|Rev)Action and invoke
       that instead.
     * When using '/' or '?' as part of a motion (e.g. "d/foo"), the above happens again, and all is good. Because
       the text has been applied as an argument on the last command, '.' will correctly repeat it.

     It's hard to see how to improve this. Removing EX_STRING means starting ex input has to happen in ExEntryAction
     and SearchEntry(Fwd|Rev)Action, and the ex command invoked in ProcessExEntryAction, but that breaks any initial
     operator, which would be invoked first (e.g. 'd' in "d/foo").
  */
        logger.trace("Processing ex_string")
        val text = injector.processGroup.endSearchCommand()
        commandBuilder.popCommandPart() // Pop ProcessExEntryAction
        commandBuilder.completeCommandPart(Argument(text)) // Set search text on SearchEntry(Fwd|Rev)Action
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
    key: Char,
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

      Argument.Type.EX_STRING -> {
        // The current Command expects an EX_STRING argument. E.g. SearchEntry(Fwd|Rev)Action. This won't execute until
        // state hits READY. Start the ex input field, push CMD_LINE mode and wait for the argument.
        injector.redrawService.redrawStatusLine()
        injector.processGroup.startSearchCommand(editor, context, commandBuilder.count, key)
        commandBuilder.commandState = CurrentCommandState.NEW_COMMAND
        val currentMode = editorState.mode
        check(currentMode is ReturnableFromCmd) { "Cannot enable command line mode $currentMode" }
        editor.mode = Mode.CMD_LINE(currentMode)
      }

      else -> Unit
    }

    // Another special case. Force a mode change to update the caret shape
    // This was a typed solution
    // if (action is ChangeCharacterAction || action is ChangeVisualCharacterAction)
    if (action.id == "VimChangeCharacterAction" || action.id == "VimChangeVisualCharacterAction") {
      editor.isReplaceCharacter = true
    }
  }

  private fun checkArgumentCompatibility(
    expectedArgumentType: Argument.Type?,
    action: EditorActionHandlerBase,
  ): Boolean {
    return !(expectedArgumentType === Argument.Type.MOTION && action.type !== Command.Type.MOTION)
  }
}
