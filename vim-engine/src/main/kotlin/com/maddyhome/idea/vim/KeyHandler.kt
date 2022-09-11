/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimActionsInitiator
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandBuilder
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MappingState
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
import com.maddyhome.idea.vim.key.KeyMappingLayer
import com.maddyhome.idea.vim.key.KeyStack
import com.maddyhome.idea.vim.key.Node
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.util.function.Consumer
import javax.swing.KeyStroke

data class KeyRoundInfo(
  var commandState: CurrentCommandState,
)

/**
 * This handles every keystroke that the user can argType except those that are still valid hotkeys for various Idea
 * actions. This is a singleton.
 */
class KeyHandler {

  private var handleKeyRecursionCount = 0

  val keyStack = KeyStack()
  val modalEntryKeys: MutableList<KeyStroke> = ArrayList()

  fun handleKeyInitial(editor: VimEditor, key: KeyStroke, context: ExecutionContext, execute: Boolean = true): StateUpdateResult {
    if (editor.vimStateMachine.commandBuilder.isReady) {
      editor.vimStateMachine.commandBuilder.resetAll(getKeyRoot(editor.vimStateMachine.mappingState.mappingMode))
    }
    return handleKey(editor, key, context, execute)
  }

  /**
   * This is the main key handler for the Vim plugin. Every keystroke not handled directly by Idea is sent here for
   * processing.
   *
   * @param editor  The editor the key was typed into
   * @param key     The keystroke typed by the user
   * @param context The data context
   */
  fun handleKey(editor: VimEditor, key: KeyStroke, context: ExecutionContext, execute: Boolean = true): StateUpdateResult {
    return handleKey(editor, key, context, allowKeyMappings = true, mappingCompleted = false, execute)
  }

  data class StateUpdateResult(
    val continueExecution: Boolean,
    val shouldRecord: Boolean,
  )

  /**
   * Handling input keys with additional parameters
   *
   * @param allowKeyMappings - If we allow key mappings or not
   * @param mappingCompleted - if true, we don't check if the mapping is incomplete
   *
   * TODO mappingCompleted and recursionCounter - we should find a more beautiful way to use them
   */
  fun handleKey(
    editor: VimEditor,
    key: KeyStroke,
    context: ExecutionContext,
    allowKeyMappings: Boolean,
    mappingCompleted: Boolean,
    execute: Boolean = true,
    directTyping: Boolean = false
  ): StateUpdateResult {
    LOG.trace {
      """
        ------- Key Handler -------
        Start key processing. allowKeyMappings: $allowKeyMappings, mappingCompleted: $mappingCompleted
        Key: $key
      """.trimIndent()
    }
    val mapMapDepth = (
      injector.optionService.getOptionValue(
        OptionScope.GLOBAL,
        OptionConstants.maxmapdepthName,
        OptionConstants.maxmapdepthName
      ) as VimInt
      ).value
    if (handleKeyRecursionCount >= mapMapDepth) {
      injector.messages.showStatusBarMessage(injector.messages.message("E223"))
      injector.messages.indicateError()
      LOG.warn("Key handling, maximum recursion of the key received. maxdepth=$mapMapDepth")
      return StateUpdateResult(false, false)
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
      if (!allowKeyMappings || !handleKeyMapping(editor, key, context, mappingCompleted)) {
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
              shouldRecord = injector.changeGroup.processKey(editor, context, key, directTyping) && shouldRecord
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
    if (execute) {
      finishedCommandPreparation(editor, context, key, shouldRecord)
    }
    return StateUpdateResult(true, shouldRecord)
  }

  fun finishedCommandPreparation(
    editor: VimEditor,
    context: ExecutionContext,
    key: KeyStroke?,
    shouldRecord: Boolean,
  ) {
    val editorState = editor.vimStateMachine
    val commandBuilder = editorState.commandBuilder
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
   * See the description for [com.maddyhome.idea.vim.action.DuplicableOperatorAction]
   */
  private fun mapOpCommand(
    key: KeyStroke,
    node: Node<VimActionsInitiator>?,
    editorState: VimStateMachine,
  ): Node<VimActionsInitiator>? {
    return if (editorState.isDuplicateOperatorKeyStroke(key)) {
      editorState.commandBuilder.getChildNode(KeyStroke.getKeyStroke('_'))
    } else node
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
            "", null
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

  private fun handleKeyMapping(
    editor: VimEditor,
    key: KeyStroke,
    context: ExecutionContext,
    mappingCompleted: Boolean,
  ): Boolean {
    LOG.debug("Start processing key mappings.")
    val commandState = editor.vimStateMachine
    val mappingState = commandState.mappingState
    val commandBuilder = commandState.commandBuilder
    if (commandBuilder.isAwaitingCharOrDigraphArgument() ||
      commandBuilder.isBuildingMultiKeyCommand() ||
      isMappingDisabledForKey(key, commandState) ||
      commandState.isRegisterPending
    ) {
      LOG.debug("Finish key processing, returning false")
      return false
    }
    mappingState.stopMappingTimer()

    // Save the unhandled key strokes until we either complete or abandon the sequence.
    LOG.trace("Add key to mapping state")
    mappingState.addKey(key)
    val mapping = injector.keyGroup.getKeyMappingLayer(mappingState.mappingMode)
    LOG.trace { "Get keys for mapping mode. mode = " + mappingState.mappingMode }

    // Returns true if any of these methods handle the key. False means that the key is unrelated to mapping and should
    // be processed as normal.
    val mappingProcessed =
      handleUnfinishedMappingSequence(editor, mappingState, mapping, mappingCompleted) ||
        handleCompleteMappingSequence(editor, context, mappingState, mapping, key) ||
        handleAbandonedMappingSequence(editor, mappingState, context)
    LOG.debug { "Finish mapping processing. Return $mappingProcessed" }

    return mappingProcessed
  }

  private fun isMappingDisabledForKey(key: KeyStroke, vimStateMachine: VimStateMachine): Boolean {
    // "0" can be mapped, but the mapping isn't applied when entering a count. Other digits are always mapped, even when
    // entering a count.
    // See `:help :map-modes`
    val isMappingDisabled = key.keyChar == '0' && vimStateMachine.commandBuilder.count > 0
    LOG.debug { "Mapping disabled for key: $isMappingDisabled" }
    return isMappingDisabled
  }

  private fun handleUnfinishedMappingSequence(
    editor: VimEditor,
    mappingState: MappingState,
    mapping: KeyMappingLayer,
    mappingCompleted: Boolean,
  ): Boolean {
    LOG.trace("Processing unfinished mappings...")
    if (mappingCompleted) {
      LOG.trace("Mapping is already completed. Returning false.")
      return false
    }

    // Is there at least one mapping that starts with the current sequence? This does not include complete matches,
    // unless a sequence is also a prefix for another mapping. We eagerly evaluate the shortest mapping, so even if a
    // mapping is a prefix, it will get evaluated when the next character is entered.
    // Note that currentlyUnhandledKeySequence is the same as the state after commandState.getMappingKeys().add(key). It
    // would be nice to tidy ths up
    if (!mapping.isPrefix(mappingState.keys)) {
      LOG.debug("There are no mappings that start with the current sequence. Returning false.")
      return false
    }

    // If the timeout option is set, set a timer that will abandon the sequence and replay the unhandled keys unmapped.
    // Every time a key is pressed and handled, the timer is stopped. E.g. if there is a mapping for "dweri", and the
    // user has typed "dw" wait for the timeout, and then replay "d" and "w" without any mapping (which will of course
    // delete a word)
    if (injector.optionService
      .isSet(OptionScope.LOCAL(editor), OptionConstants.timeoutName, OptionConstants.timeoutName)
    ) {
      LOG.trace("Timeout is set. Schedule a mapping timer")
      // XXX There is a strange issue that reports that mapping state is empty at the moment of the function call.
      //   At the moment, I see the only one possibility this to happen - other key is handled after the timer executed,
      //   but before invoke later is handled. This is a rare case, so I'll just add a check to isPluginMapping.
      //   But this "unexpected behaviour" exists and it would be better not to relay on mutable state with delays.
      //   https://youtrack.jetbrains.com/issue/VIM-2392
      mappingState.startMappingTimer {
        injector.application.invokeLater(
          {
            LOG.debug("Delayed mapping timer call")
            val unhandledKeys = mappingState.detachKeys()
            if (editor.isDisposed() || isPluginMapping(unhandledKeys)) {
              LOG.debug("Abandon mapping timer")
              return@invokeLater
            }
            LOG.trace("Processing unhandled keys...")
            for (keyStroke in unhandledKeys) {
              handleKey(
                editor, keyStroke, injector.executionContextManager.onEditor(editor),
                allowKeyMappings = true,
                mappingCompleted = true
              )
            }
          }, editor
        )
      }
    }
    LOG.trace("Unfinished mapping processing finished")
    return true
  }

  private fun handleCompleteMappingSequence(
    editor: VimEditor,
    context: ExecutionContext,
    mappingState: MappingState,
    mapping: KeyMappingLayer,
    key: KeyStroke,
  ): Boolean {
    LOG.trace("Processing complete mapping sequence...")
    // The current sequence isn't a prefix, check to see if it's a completed sequence.
    val currentMappingInfo = mapping.getLayer(mappingState.keys)
    var mappingInfo = currentMappingInfo
    if (mappingInfo == null) {
      LOG.trace("Haven't found any mapping info for the given sequence. Trying to apply mapping to a subsequence.")
      // It's an abandoned sequence, check to see if the previous sequence was a complete sequence.
      // TODO: This is incorrect behaviour
      // What about sequences that were completed N keys ago?
      // This should really be handled as part of an abandoned key sequence. We should also consolidate the replay
      // of cached keys - this happens in timeout, here and also in abandoned sequences.
      // Extract most of this method into handleMappingInfo. If we have a complete sequence, call it and we're done.
      // If it's not a complete sequence, handleAbandonedMappingSequence should do something like call
      // mappingState.detachKeys and look for the longest complete sequence in the returned list, evaluate it, and then
      // replay any keys not yet handled. NB: The actual implementation should be compared to Vim behaviour to see what
      // should actually happen.
      val previouslyUnhandledKeySequence = ArrayList<KeyStroke>()
      mappingState.keys.forEach(Consumer { e: KeyStroke -> previouslyUnhandledKeySequence.add(e) })
      if (previouslyUnhandledKeySequence.size > 1) {
        previouslyUnhandledKeySequence.removeAt(previouslyUnhandledKeySequence.size - 1)
        mappingInfo = mapping.getLayer(previouslyUnhandledKeySequence)
      }
    }
    if (mappingInfo == null) {
      LOG.trace("Cannot find any mapping info for the sequence. Return false.")
      return false
    }
    mappingState.resetMappingSequence()
    val currentContext = context.updateEditor(editor)
    LOG.trace("Executing mapping info")
    try {
      mappingState.startMapExecution()
      mappingInfo.execute(editor, context)
    } catch (e: Exception) {
      injector.messages.showStatusBarMessage(e.message)
      injector.messages.indicateError()
      LOG.warn(
        """
                Caught exception during ${mappingInfo.getPresentableString()}
                ${e.message}
        """.trimIndent()
      )
    } catch (e: NotImplementedError) {
      injector.messages.showStatusBarMessage(e.message)
      injector.messages.indicateError()
      LOG.warn(
        """
                 Caught exception during ${mappingInfo.getPresentableString()}
                 ${e.message}
        """.trimIndent()
      )
    } finally {
      mappingState.stopMapExecution()
    }

    // If we've just evaluated the previous key sequence, make sure to also handle the current key
    if (mappingInfo !== currentMappingInfo) {
      LOG.trace("Evaluating the current key")
      handleKey(editor, key, currentContext, allowKeyMappings = true, false)
    }
    LOG.trace("Success processing of mapping")
    return true
  }

  private fun handleAbandonedMappingSequence(
    editor: VimEditor,
    mappingState: MappingState,
    context: ExecutionContext,
  ): Boolean {
    LOG.debug("Processing abandoned mapping sequence")
    // The user has terminated a mapping sequence with an unexpected key
    // E.g. if there is a mapping for "hello" and user enters command "help" the processing of "h", "e" and "l" will be
    //   prevented by this handler. Make sure the currently unhandled keys are processed as normal.
    val unhandledKeyStrokes = mappingState.detachKeys()

    // If there is only the current key to handle, do nothing
    if (unhandledKeyStrokes.size == 1) {
      LOG.trace("There is only one key in mapping. Return false.")
      return false
    }

    // Okay, look at the code below. Why is the first key handled separately?
    // Let's assume the next mappings:
    //   - map ds j
    //   - map I 2l
    // If user enters `dI`, the first `d` will be caught be this handler because it's a prefix for `ds` command.
    //  After the user enters `I`, the caught `d` should be processed without mapping, and the rest of keys
    //  should be processed with mappings (to make I work)
    if (isPluginMapping(unhandledKeyStrokes)) {
      LOG.trace("This is a plugin mapping, process it")
      handleKey(
        editor, unhandledKeyStrokes[unhandledKeyStrokes.size - 1], context,
        allowKeyMappings = true,
        mappingCompleted = false
      )
    } else {
      LOG.trace("Process abandoned keys.")
      handleKey(editor, unhandledKeyStrokes[0], context, allowKeyMappings = false, mappingCompleted = false)
      for (keyStroke in unhandledKeyStrokes.subList(1, unhandledKeyStrokes.size)) {
        handleKey(editor, keyStroke, context, allowKeyMappings = true, mappingCompleted = false)
      }
    }
    LOG.trace("Return true from abandoned keys processing.")
    return true
  }

  // The <Plug>mappings are not executed if they fail to map to something.
  //   E.g.
  //   - map <Plug>iA someAction
  //   - map I <Plug>i
  //   For `IA` someAction should be executed.
  //   But if the user types `Ib`, `<Plug>i` won't be executed again. Only `b` will be passed to keyHandler.
  private fun isPluginMapping(unhandledKeyStrokes: List<KeyStroke>): Boolean {
    return unhandledKeyStrokes.isNotEmpty() && unhandledKeyStrokes[0] == injector.parser.plugKeyStroke
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
    } else key.keyChar == '"' && !editorState.isOperatorPending && editorState.commandBuilder.expectedArgumentType == null
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
        // UNHANDLED means the key stroke made no sense in the context of a digraph, but isn't an error in the current
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
    val command = editorState.commandBuilder.buildCommand(context)
    val operatorArguments = OperatorArguments(
      editorState.mappingState.mappingMode == MappingMode.OP_PENDING,
      command.rawCount, editorState.mode, editorState.subMode
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
  fun partialReset(editor: VimEditor) {
    val editorState = getInstance(editor)
    editorState.mappingState.resetMappingSequence()
    editorState.commandBuilder.resetInProgressCommandPart(getKeyRoot(editorState.mappingState.mappingMode))
  }

  /**
   * Resets the state of this handler. Does a partial reset then resets the mode, the command, and the argument.
   *
   * @param editor The editor to reset.
   */
  fun reset(editor: VimEditor) {
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
  fun fullReset(editor: VimEditor) {
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
      if (!context.isNewDelegate()) {
        editorState.commandBuilder.commandState = CurrentCommandState.NEW_COMMAND
      }
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

  companion object {
    private val LOG: VimLogger = vimLogger<KeyHandler>()

    fun <T> isPrefix(list1: List<T>, list2: List<T>): Boolean {
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
    fun getInstance() = instance
  }
}
