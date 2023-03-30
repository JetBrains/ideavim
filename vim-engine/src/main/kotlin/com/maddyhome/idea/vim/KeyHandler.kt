/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimActionsInitiator
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandBuilder
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MappingProcessor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.command.VimStateMachine.Companion.getInstance
import com.maddyhome.idea.vim.common.CurrentCommandState
import com.maddyhome.idea.vim.common.DigraphResult
import com.maddyhome.idea.vim.common.argumentCaptured
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.helper.inSingleNormalMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.isCloseKeyStroke
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.key.CommandNode
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.KeyStack
import com.maddyhome.idea.vim.key.Node
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * This handles every keystroke that the user can argType except those that are still valid hotkeys for various Idea
 * actions. This is a singleton.
 */
public class KeyHandler {

  private var handleKeyRecursionCount = 0

  public val keyStack: KeyStack = KeyStack()
  public val modalEntryKeys: MutableList<KeyStroke> = ArrayList()

  /**
   * This is the main key handler for the Vim plugin. Every keystroke not handled directly by Idea is sent here for
   * processing.
   *
   * @param editor  The editor the key was typed into
   * @param key     The keystroke typed by the user
   * @param context The data context
   */
  public fun handleKey(editor: VimEditor, key: KeyStroke, context: ExecutionContext) {
    handleKey(editor, key, context, allowKeyMappings = true, mappingCompleted = false)
  }

  /**
   * Handling input keys with additional parameters
   *
   * @param allowKeyMappings - If we allow key mappings or not
   * @param mappingCompleted - if true, we don't check if the mapping is incomplete
   *
   * TODO mappingCompleted and recursionCounter - we should find a more beautiful way to use them
   */
  public fun handleKey(
    editor: VimEditor,
    key: KeyStroke,
    context: ExecutionContext,
    allowKeyMappings: Boolean,
    mappingCompleted: Boolean,
  ) {
    LOG.trace {
      """
        ------- Key Handler -------
        Start key processing. allowKeyMappings: $allowKeyMappings, mappingCompleted: $mappingCompleted
        Key: $key
      """.trimIndent()
    }
    val maxMapDepth = injector.globalOptions().maxmapdepth
    if (handleKeyRecursionCount >= maxMapDepth) {
      injector.messages.showStatusBarMessage(editor, injector.messages.message("E223"))
      injector.messages.indicateError()
      LOG.warn("Key handling, maximum recursion of the key received. maxdepth=$maxMapDepth")
      return
    }

    injector.messages.clearError()
    val editorState = editor.vimStateMachine
    val commandBuilder = editorState.commandBuilder

    // If this is a "regular" character keystroke, get the character
    val chKey: Char = if (key.keyChar == KeyEvent.CHAR_UNDEFINED) 0.toChar() else key.keyChar

    // We only record unmapped keystrokes. If we've recursed to handle mapping, don't record anything.
    var shouldRecord = handleKeyRecursionCount == 0 && editorState.isRecording
    handleKeyRecursionCount++
    try {
      LOG.trace("Start key processing...")
      if (!allowKeyMappings || !MappingProcessor.handleKeyMapping(editor, key, context, mappingCompleted)) {
        LOG.trace("Mappings processed, continue processing key.")
        if (isCommandCountKey(chKey, editorState)) {
          commandBuilder.addCountCharacter(key)
        } else if (isDeleteCommandCountKey(key, editorState)) {
          commandBuilder.deleteCountCharacter()
        } else if (isEditorReset(key, editorState)) {
          handleEditorReset(editor, key, context, editorState)
        } else if (isExpectingCharArgument(commandBuilder)) {
          handleCharArgument(key, chKey, editorState)
        } else if (editorState.isRegisterPending) {
          LOG.trace("Pending mode.")
          commandBuilder.addKey(key)
          handleSelectRegister(editorState, chKey)
        } else if (!handleDigraph(editor, key, context, editorState)) {
          LOG.debug("Digraph is NOT processed")

          // Ask the key/action tree if this is an appropriate key at this point in the command and if so,
          // return the node matching this keystroke
          val node: Node<VimActionsInitiator>? = mapOpCommand(key, commandBuilder.getChildNode(key), editorState)
          LOG.trace("Get the node for the current mode")

          if (node is CommandNode<VimActionsInitiator>) {
            LOG.trace("Node is a command node")
            handleCommandNode(editor, context, key, node, editorState)
            commandBuilder.addKey(key)
          } else if (node is CommandPartNode<VimActionsInitiator>) {
            LOG.trace("Node is a command part node")
            commandBuilder.setCurrentCommandPartNode(node)
            commandBuilder.addKey(key)
          } else if (isSelectRegister(key, editorState)) {
            LOG.trace("Select register")
            editorState.isRegisterPending = true
            commandBuilder.addKey(key)
          } else {
            // node == null
            LOG.trace("We are not able to find a node for this key")

            // If we are in insert/replace mode send this key in for processing
            if (editorState.mode == VimStateMachine.Mode.INSERT || editorState.mode == VimStateMachine.Mode.REPLACE) {
              LOG.trace("Process insert or replace")
              shouldRecord = injector.changeGroup.processKey(editor, context, key) && shouldRecord
            } else if (editorState.mode == VimStateMachine.Mode.SELECT) {
              LOG.trace("Process select")
              shouldRecord = injector.changeGroup.processKeyInSelectMode(editor, context, key) && shouldRecord
            } else if (editorState.mappingState.mappingMode == MappingMode.CMD_LINE) {
              LOG.trace("Process cmd line")
              shouldRecord = injector.processGroup.processExKey(editor, key) && shouldRecord
            } else {
              LOG.trace("Set command state to bad_command")
              commandBuilder.commandState = CurrentCommandState.BAD_COMMAND
            }
            partialReset(editor)
          }
        }
      }
    } finally {
      handleKeyRecursionCount--
    }
    finishedCommandPreparation(editor, context, editorState, commandBuilder, key, shouldRecord)
  }

  internal fun finishedCommandPreparation(
    editor: VimEditor,
    context: ExecutionContext,
    editorState: VimStateMachine,
    commandBuilder: CommandBuilder,
    key: KeyStroke?,
    shouldRecord: Boolean,
  ) {
    // Do we have a fully entered command at this point? If so, let's execute it.
    if (commandBuilder.isReady) {
      LOG.trace("Ready command builder. Execute command.")
      executeCommand(editor, context, editorState)
    } else if (commandBuilder.isBad) {
      LOG.trace("Command builder is set to BAD")
      editorState.resetOpPending()
      editorState.resetRegisterPending()
      editorState.resetReplaceCharacter()
      injector.messages.indicateError()
      reset(editor)
    }

    // Don't record the keystroke that stops the recording (unmapped this is `q`)
    if (shouldRecord && editorState.isRecording && key != null) {
      injector.registerGroup.recordKeyStroke(key)
      modalEntryKeys.forEach { injector.registerGroup.recordKeyStroke(it) }
      modalEntryKeys.clear()
    }

    // This will update immediately, if we're on the EDT (which we are)
    injector.messages.updateStatusBar()
    LOG.trace("----------- Key Handler Finished -----------")
  }

  /**
   * See the description for [com.maddyhome.idea.vim.command.DuplicableOperatorAction]
   */
  private fun mapOpCommand(
    key: KeyStroke,
    node: Node<VimActionsInitiator>?,
    editorState: VimStateMachine,
  ): Node<VimActionsInitiator>? {
    return if (editorState.isDuplicateOperatorKeyStroke(key)) {
      editorState.commandBuilder.getChildNode(KeyStroke.getKeyStroke('_'))
    } else {
      node
    }
  }

  private fun handleEditorReset(
    editor: VimEditor,
    key: KeyStroke,
    context: ExecutionContext,
    editorState: VimStateMachine,
  ) {
    val commandBuilder = editorState.commandBuilder
    if (commandBuilder.isAwaitingCharOrDigraphArgument()) {
      editorState.resetReplaceCharacter()
    }
    if (commandBuilder.isAtDefaultState) {
      val register = injector.registerGroup
      if (register.currentRegister == register.defaultRegister) {
        var indicateError = true
        if (key.keyCode == KeyEvent.VK_ESCAPE) {
          val executed = arrayOf<Boolean?>(null)
          injector.actionExecutor.executeCommand(
            editor,
            { executed[0] = injector.actionExecutor.executeEsc(context) },
            "",
            null,
          )
          indicateError = !executed[0]!!
        }
        if (indicateError) {
          injector.messages.indicateError()
        }
      }
    }
    reset(editor)
  }

  private fun isCommandCountKey(chKey: Char, editorState: VimStateMachine): Boolean {
    // Make sure to avoid handling '0' as the start of a count.
    val commandBuilder = editorState.commandBuilder
    val notRegisterPendingCommand = editorState.mode.inNormalMode && !editorState.isRegisterPending
    val visualMode = editorState.mode.inVisualMode && !editorState.isRegisterPending
    val opPendingMode = editorState.mode === VimStateMachine.Mode.OP_PENDING

    if (notRegisterPendingCommand || visualMode || opPendingMode) {
      if (commandBuilder.isExpectingCount && Character.isDigit(chKey) && (commandBuilder.count > 0 || chKey != '0')) {
        LOG.debug("This is a command key count")
        return true
      }
    }
    LOG.debug("This is NOT a command key count")
    return false
  }

  private fun isDeleteCommandCountKey(key: KeyStroke, editorState: VimStateMachine): Boolean {
    // See `:help N<Del>`
    val commandBuilder = editorState.commandBuilder
    val isDeleteCommandKeyCount =
      (editorState.mode === VimStateMachine.Mode.COMMAND || editorState.mode === VimStateMachine.Mode.VISUAL || editorState.mode === VimStateMachine.Mode.OP_PENDING) &&
        commandBuilder.isExpectingCount && commandBuilder.count > 0 && key.keyCode == KeyEvent.VK_DELETE

    LOG.debug { "This is a delete command key count: $isDeleteCommandKeyCount" }
    return isDeleteCommandKeyCount
  }

  private fun isEditorReset(key: KeyStroke, editorState: VimStateMachine): Boolean {
    val editorReset = editorState.mode == VimStateMachine.Mode.COMMAND && key.isCloseKeyStroke()
    LOG.debug { "This is editor reset: $editorReset" }
    return editorReset
  }

  private fun isSelectRegister(key: KeyStroke, editorState: VimStateMachine): Boolean {
    if (editorState.mode != VimStateMachine.Mode.COMMAND && editorState.mode != VimStateMachine.Mode.VISUAL) {
      return false
    }
    return if (editorState.isRegisterPending) {
      true
    } else {
      key.keyChar == '"' && !editorState.isOperatorPending && editorState.commandBuilder.expectedArgumentType == null
    }
  }

  private fun handleSelectRegister(vimStateMachine: VimStateMachine, chKey: Char) {
    LOG.trace("Handle select register")
    vimStateMachine.resetRegisterPending()
    if (injector.registerGroup.isValid(chKey)) {
      LOG.trace("Valid register")
      vimStateMachine.commandBuilder.pushCommandPart(chKey)
    } else {
      LOG.trace("Invalid register, set command state to BAD_COMMAND")
      vimStateMachine.commandBuilder.commandState = CurrentCommandState.BAD_COMMAND
    }
  }

  private fun isExpectingCharArgument(commandBuilder: CommandBuilder): Boolean {
    val expectingCharArgument = commandBuilder.expectedArgumentType === Argument.Type.CHARACTER
    LOG.debug { "Expecting char argument: $expectingCharArgument" }
    return expectingCharArgument
  }

  private fun handleCharArgument(key: KeyStroke, chKey: Char, vimStateMachine: VimStateMachine) {
    var mutableChKey = chKey
    LOG.trace("Handling char argument")
    // We are expecting a character argument - is this a regular character the user typed?
    // Some special keys can be handled as character arguments - let's check for them here.
    if (mutableChKey.code == 0) {
      when (key.keyCode) {
        KeyEvent.VK_TAB -> mutableChKey = '\t'
        KeyEvent.VK_ENTER -> mutableChKey = '\n'
      }
    }
    val commandBuilder = vimStateMachine.commandBuilder
    if (mutableChKey.code != 0) {
      LOG.trace("Add character argument to the current command")
      // Create the character argument, add it to the current command, and signal we are ready to process the command
      commandBuilder.completeCommandPart(Argument(mutableChKey))
    } else {
      LOG.trace("This is not a valid character argument. Set command state to BAD_COMMAND")
      // Oops - this isn't a valid character argument
      commandBuilder.commandState = CurrentCommandState.BAD_COMMAND
    }
    vimStateMachine.resetReplaceCharacter()
  }

  private fun handleDigraph(
    editor: VimEditor,
    key: KeyStroke,
    context: ExecutionContext,
    editorState: VimStateMachine,
  ): Boolean {
    LOG.debug("Handling digraph")
    // Support starting a digraph/literal sequence if the operator accepts one as an argument, e.g. 'r' or 'f'.
    // Normally, we start the sequence (in Insert or CmdLine mode) through a VimAction that can be mapped. Our
    // VimActions don't work as arguments for operators, so we have to special case here. Helpfully, Vim appears to
    // hardcode the shortcuts, and doesn't support mapping, so everything works nicely.
    val commandBuilder = editorState.commandBuilder
    if (commandBuilder.expectedArgumentType == Argument.Type.DIGRAPH) {
      LOG.trace("Expected argument is digraph")
      if (editorState.digraphSequence.isDigraphStart(key)) {
        editorState.startDigraphSequence()
        editorState.commandBuilder.addKey(key)
        return true
      }
      if (editorState.digraphSequence.isLiteralStart(key)) {
        editorState.startLiteralSequence()
        editorState.commandBuilder.addKey(key)
        return true
      }
    }
    val res = editorState.processDigraphKey(key, editor)
    if (injector.exEntryPanel.isActive()) {
      when (res.result) {
        DigraphResult.RES_HANDLED -> setPromptCharacterEx(if (commandBuilder.isPuttingLiteral()) '^' else key.keyChar)
        DigraphResult.RES_DONE, DigraphResult.RES_BAD -> if (key.keyCode == KeyEvent.VK_C && key.modifiers and InputEvent.CTRL_DOWN_MASK != 0) {
          return false
        } else {
          injector.exEntryPanel.clearCurrentAction()
        }
      }
    }
    when (res.result) {
      DigraphResult.RES_HANDLED -> {
        editorState.commandBuilder.addKey(key)
        return true
      }
      DigraphResult.RES_DONE -> {
        if (commandBuilder.expectedArgumentType === Argument.Type.DIGRAPH) {
          commandBuilder.fallbackToCharacterArgument()
        }
        val stroke = res.stroke ?: return false
        editorState.commandBuilder.addKey(key)
        handleKey(editor, stroke, context)
        return true
      }
      DigraphResult.RES_BAD -> {
        // BAD is an error. We were expecting a valid character, and we didn't get it.
        if (commandBuilder.expectedArgumentType != null) {
          commandBuilder.commandState = CurrentCommandState.BAD_COMMAND
        }
        return true
      }
      DigraphResult.RES_UNHANDLED -> {
        // UNHANDLED means the keystroke made no sense in the context of a digraph, but isn't an error in the current
        // state. E.g. waiting for {char} <BS> {char}. Let the key handler have a go at it.
        if (commandBuilder.expectedArgumentType === Argument.Type.DIGRAPH) {
          commandBuilder.fallbackToCharacterArgument()
          handleKey(editor, key, context)
          return true
        }
        return false
      }
    }
    return false
  }

  private fun executeCommand(
    editor: VimEditor,
    context: ExecutionContext,
    editorState: VimStateMachine,
  ) {
    LOG.trace("Command execution")
    val command = editorState.commandBuilder.buildCommand()
    val operatorArguments = OperatorArguments(
      editorState.mappingState.mappingMode == MappingMode.OP_PENDING,
      command.rawCount,
      editorState.mode,
      editorState.subMode,
    )

    // If we were in "operator pending" mode, reset back to normal mode.
    editorState.resetOpPending()

    // Save off the command we are about to execute
    editorState.setExecutingCommand(command)
    val type = command.type
    if (type.isWrite) {
      if (!editor.isWritable()) {
        injector.messages.indicateError()
        reset(editor)
        LOG.warn("File is not writable")
        return
      }
    }
    if (injector.application.isMainThread()) {
      val action: Runnable = ActionRunner(editor, context, command, operatorArguments)
      val cmdAction = command.action
      val name = cmdAction.id
      if (type.isWrite) {
        injector.application.runWriteCommand(editor, name, action, action)
      } else if (type.isRead) {
        injector.application.runReadCommand(editor, name, action, action)
      } else {
        injector.actionExecutor.executeCommand(editor, action, name, action)
      }
    }
  }

  private fun handleCommandNode(
    editor: VimEditor,
    context: ExecutionContext,
    key: KeyStroke,
    node: CommandNode<VimActionsInitiator>,
    editorState: VimStateMachine,
  ) {
    LOG.trace("Handle command node")
    // The user entered a valid command. Create the command and add it to the stack.
    val action = node.actionHolder.getInstance()
    val commandBuilder = editorState.commandBuilder
    val expectedArgumentType = commandBuilder.expectedArgumentType
    commandBuilder.pushCommandPart(action)
    if (!checkArgumentCompatibility(expectedArgumentType, action)) {
      LOG.trace("Return from command node handling")
      commandBuilder.commandState = CurrentCommandState.BAD_COMMAND
      return
    }
    if (action.argumentType == null || stopMacroRecord(node, editorState)) {
      LOG.trace("Set command state to READY")
      commandBuilder.commandState = CurrentCommandState.READY
    } else {
      LOG.trace("Set waiting for the argument")
      val argumentType = action.argumentType
      startWaitingForArgument(editor, context, key.keyChar, action, argumentType!!, editorState)
      partialReset(editor)
    }

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
      LOG.trace("Processing ex_string")
      val text = injector.processGroup.endSearchCommand()
      commandBuilder.popCommandPart() // Pop ProcessExEntryAction
      commandBuilder.completeCommandPart(Argument(text)) // Set search text on SearchEntry(Fwd|Rev)Action
      editorState.popModes() // Pop CMD_LINE
    }
  }

  private fun stopMacroRecord(node: CommandNode<VimActionsInitiator>, editorState: VimStateMachine): Boolean {
    // TODO
//    return editorState.isRecording && node.actionHolder.getInstance() is ToggleRecordingAction
    return editorState.isRecording && node.actionHolder.getInstance().id == "VimToggleRecordingAction"
  }

  private fun startWaitingForArgument(
    editor: VimEditor,
    context: ExecutionContext,
    key: Char,
    action: EditorActionHandlerBase,
    argument: Argument.Type,
    editorState: VimStateMachine,
  ) {
    val commandBuilder = editorState.commandBuilder
    when (argument) {
      Argument.Type.MOTION -> {
        if (editorState.isDotRepeatInProgress && argumentCaptured != null) {
          commandBuilder.completeCommandPart(argumentCaptured!!)
        }
        editorState.pushModes(VimStateMachine.Mode.OP_PENDING, VimStateMachine.SubMode.NONE)
      }
      Argument.Type.DIGRAPH -> // Command actions represent the completion of a command. Showcmd relies on this - if the action represents a
        // part of a command, the showcmd output is reset part way through. This means we need to special case entering
        // digraph/literal input mode. We have an action that takes a digraph as an argument, and pushes it back through
        // the key handler when it's complete.

        // TODO
//        if (action is InsertCompletedDigraphAction) {
        if (action.id == "VimInsertCompletedDigraphAction") {
          editorState.startDigraphSequence()
          setPromptCharacterEx('?')
        } else if (action.id == "VimInsertCompletedLiteralAction") {
          editorState.startLiteralSequence()
          setPromptCharacterEx('^')
        }
      Argument.Type.EX_STRING -> {
        // The current Command expects an EX_STRING argument. E.g. SearchEntry(Fwd|Rev)Action. This won't execute until
        // state hits READY. Start the ex input field, push CMD_LINE mode and wait for the argument.
        injector.processGroup.startSearchCommand(editor, context, commandBuilder.count, key)
        commandBuilder.commandState = CurrentCommandState.NEW_COMMAND
        editorState.pushModes(VimStateMachine.Mode.CMD_LINE, VimStateMachine.SubMode.NONE)
      }
      else -> Unit
    }

    // Another special case. Force a mode change to update the caret shape
    // This was a typed solution
    // if (action is ChangeCharacterAction || action is ChangeVisualCharacterAction)
    if (action.id == "VimChangeCharacterAction" || action.id == "VimChangeVisualCharacterAction") {
      editorState.isReplaceCharacter = true
    }
  }

  private fun checkArgumentCompatibility(
    expectedArgumentType: Argument.Type?,
    action: EditorActionHandlerBase,
  ): Boolean {
    return !(expectedArgumentType === Argument.Type.MOTION && action.type !== Command.Type.MOTION)
  }

  /**
   * Partially resets the state of this handler. Resets the command count, clears the key list, resets the key tree
   * node to the root for the current mode we are in.
   *
   * @param editor The editor to reset.
   */
  public fun partialReset(editor: VimEditor) {
    val editorState = getInstance(editor)
    editorState.mappingState.resetMappingSequence()
    editorState.commandBuilder.resetInProgressCommandPart(getKeyRoot(editorState.mappingState.mappingMode))
  }

  /**
   * Resets the state of this handler. Does a partial reset then resets the mode, the command, and the argument.
   *
   * @param editor The editor to reset.
   */
  public fun reset(editor: VimEditor) {
    partialReset(editor)
    val editorState = getInstance(editor)
    editorState.commandBuilder.resetAll(getKeyRoot(editorState.mappingState.mappingMode))
  }

  private fun getKeyRoot(mappingMode: MappingMode): CommandPartNode<VimActionsInitiator> {
    return injector.keyGroup.getKeyRoot(mappingMode)
  }

  /**
   * Completely resets the state of this handler. Resets the command mode to normal, resets, and clears the selected
   * register.
   *
   * @param editor The editor to reset.
   */
  public fun fullReset(editor: VimEditor) {
    injector.messages.clearError()
    getInstance(editor).reset()
    reset(editor)
    injector.registerGroupIfCreated?.resetRegister()
    editor.removeSelection()
  }

  private fun setPromptCharacterEx(promptCharacter: Char) {
    val exEntryPanel = injector.exEntryPanel
    if (exEntryPanel.isActive()) {
      exEntryPanel.setCurrentActionPromptCharacter(promptCharacter)
    }
  }

  /**
   * This was used as an experiment to execute actions as a runnable.
   */
  internal class ActionRunner(
    val editor: VimEditor,
    val context: ExecutionContext,
    val cmd: Command,
    val operatorArguments: OperatorArguments,
  ) : Runnable {
    override fun run() {
      val editorState = getInstance(editor)
      editorState.commandBuilder.commandState = CurrentCommandState.NEW_COMMAND
      val register = cmd.register
      if (register != null) {
        injector.registerGroup.selectRegister(register)
      }
      injector.actionExecutor.executeVimAction(editor, cmd.action, context, operatorArguments)
      if (editorState.mode === VimStateMachine.Mode.INSERT || editorState.mode === VimStateMachine.Mode.REPLACE) {
        injector.changeGroup.processCommand(editor, cmd)
      }

      // Now the command has been executed let's clean up a few things.

      // By default, the "empty" register is used by all commands, so we want to reset whatever the last register
      // selected by the user was to the empty register
      injector.registerGroup.resetRegister()

      // If, at this point, we are not in insert, replace, or visual modes, we need to restore the previous
      // mode we were in. This handles commands in those modes that temporarily allow us to execute normal
      // mode commands. An exception is if this command should leave us in the temporary mode such as
      // "select register"
      if (editorState.mode.inSingleNormalMode &&
        !cmd.flags.contains(CommandFlags.FLAG_EXPECT_MORE)
      ) {
        editorState.popModes()
      }
      if (editorState.commandBuilder.isDone()) {
        getInstance().reset(editor)
      }
    }
  }

  public companion object {
    private val LOG: VimLogger = vimLogger<KeyHandler>()

    internal fun <T> isPrefix(list1: List<T>, list2: List<T>): Boolean {
      if (list1.size > list2.size) {
        return false
      }
      for (i in list1.indices) {
        if (list1[i] != list2[i]) {
          return false
        }
      }
      return true
    }

    private val instance = KeyHandler()

    @JvmStatic
    public fun getInstance(): KeyHandler = instance
  }
}
