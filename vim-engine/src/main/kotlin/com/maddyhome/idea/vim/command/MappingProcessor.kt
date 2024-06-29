/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.impl.state.toMappingMode
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.key.KeyMappingLayer
import com.maddyhome.idea.vim.key.MappingInfoLayer
import com.maddyhome.idea.vim.state.KeyHandlerState
import javax.swing.KeyStroke

object MappingProcessor: KeyConsumer {

  private val log = vimLogger<MappingProcessor>()

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    mappingCompleted: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
    shouldRecord: KeyHandler.MutableBoolean,
  ): Boolean {
    log.trace { "Entered MappingProcessor" }
    if (!allowKeyMappings) return false

    log.debug("Start processing key mappings.")
    val keyState = keyProcessResultBuilder.state
    val mappingState = keyState.mappingState
    val commandBuilder = keyState.commandBuilder
    if (commandBuilder.isAwaitingCharOrDigraphArgument() ||
      commandBuilder.isBuildingMultiKeyCommand() ||
      isMappingDisabledForKey(key, keyState) ||
      injector.vimState.isRegisterPending
    ) {
      log.debug("Finish key processing, returning false")
      return false
    }
    mappingState.stopMappingTimer()

    // Save the unhandled keystrokes until we either complete or abandon the sequence.
    log.trace("Add key to mapping state")
    mappingState.addKey(key)
    val mappingMode = editor.mode.toMappingMode()
    val mapping = injector.keyGroup.getKeyMappingLayer(mappingMode)
    log.trace { "Get keys for mapping mode. mode = $mappingMode" }

    // Returns true if any of these methods handle the key. False means that the key is unrelated to mapping and should
    // be processed as normal.
    val mappingProcessed =
      handleUnfinishedMappingSequence(keyProcessResultBuilder, mapping, mappingCompleted) ||
        handleCompleteMappingSequence(keyProcessResultBuilder, mapping, key) ||
        handleAbandonedMappingSequence(keyProcessResultBuilder)
    log.debug { "Finish mapping processing. Return $mappingProcessed" }

    return mappingProcessed
  }

  private fun isMappingDisabledForKey(key: KeyStroke, keyState: KeyHandlerState): Boolean {
    // "0" can be mapped, but the mapping isn't applied when entering a count. Other digits are always mapped, even when
    // entering a count.
    // See `:help :map-modes`
    val isMappingDisabled = key.keyChar == '0' && keyState.commandBuilder.count > 0
    log.debug { "Mapping disabled for key: $isMappingDisabled" }
    return isMappingDisabled
  }

  private fun handleUnfinishedMappingSequence(
    processBuilder: KeyProcessResult.KeyProcessResultBuilder,
    mapping: KeyMappingLayer,
    mappingCompleted: Boolean,
  ): Boolean {
    log.trace("processing unfinished mappings...")
    if (mappingCompleted) {
      log.trace("mapping is already completed. Returning false.")
      return false
    }

    // Is there at least one mapping that starts with the current sequence? This does not include complete matches,
    // unless a sequence is also a prefix for another mapping. We eagerly evaluate the shortest mapping, so even if a
    // mapping is a prefix, it will get evaluated when the next character is entered.
    // Note that currentlyUnhandledKeySequence is the same as the state after commandState.getMappingKeys().add(key). It
    // would be nice to tidy ths up
    if (!mapping.isPrefix(processBuilder.state.mappingState.keys)) {
      log.debug("There are no mappings that start with the current sequence. Returning false.")
      return false
    }

    // If the timeout option is set, set a timer that will abandon the sequence and replay the unhandled keys unmapped.
    // Every time a key is pressed and handled, the timer is stopped. E.g. if there is a mapping for "dweri", and the
    // user has typed "dw" wait for the timeout, and then replay "d" and "w" without any mapping (which will of course
    // delete a word)
    processBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, _ -> processUnfinishedMappingSequence(lambdaEditor, lambdaKeyState) }
    return true
  }

  private fun processUnfinishedMappingSequence(editor: VimEditor, keyState: KeyHandlerState) {
    if (injector.options(editor).timeout) {
      log.trace("timeout is set. schedule a mapping timer")
      // XXX There is a strange issue that reports that mapping state is empty at the moment of the function call.
      //   At the moment, I see the only one possibility this to happen - other key is handled after the timer executed,
      //   but before invoke later is handled. This is a rare case, so I'll just add a check to isPluginMapping.
      //   But this "unexpected behaviour" exists, and it would be better not to relay on mutable state with delays.
      //   https://youtrack.jetbrains.com/issue/VIM-2392
      val mappingState = keyState.mappingState
      mappingState.startMappingTimer {
        injector.application.invokeLater(
          {
            log.debug("Delayed mapping timer call")
            val unhandledKeys = mappingState.detachKeys()
            if (editor.isDisposed() || isPluginMapping(unhandledKeys)) {
              log.debug("Abandon mapping timer")
              return@invokeLater
            }
            log.trace("processing unhandled keys...")
            for ((index, keyStroke) in unhandledKeys.withIndex()) {
              // Related issue: VIM-2315
              // If we have two mappings: for `abc` and for `ab`, after typing `ab` we should wait a bit and execute
              //   `ab` mapping
              // So, we rerun all keys with mappings with "mappingsCompleted" for the last char to avoid infinite loop
              //  of waiting for `abc` mapping.
              val lastKeyInSequence = index == unhandledKeys.lastIndex

              val keyHandler = KeyHandler.getInstance()
              keyHandler.handleKey(
                editor,
                keyStroke,
                injector.executionContextManager.getEditorExecutionContext(editor),
                allowKeyMappings = true,
                mappingCompleted = lastKeyInSequence,
                keyState,
              )
            }
          },
          editor,
        )
      }
    }
    log.trace("Unfinished mapping processing finished")
  }

  private fun handleCompleteMappingSequence(
    processBuilder: KeyProcessResult.KeyProcessResultBuilder,
    mapping: KeyMappingLayer,
    key: KeyStroke,
  ): Boolean {
    log.trace("Processing complete mapping sequence...")
    // The current sequence isn't a prefix, check to see if it's a completed sequence.
    val mappingState = processBuilder.state.mappingState
    val currentMappingInfo = mapping.getLayer(mappingState.keys)
    var mappingInfo = currentMappingInfo
    if (mappingInfo == null) {
      log.trace("Haven't found any mapping info for the given sequence. Trying to apply mapping to a subsequence.")
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
      mappingState.keys.forEach { e: KeyStroke -> previouslyUnhandledKeySequence.add(e) }
      if (previouslyUnhandledKeySequence.size > 1) {
        previouslyUnhandledKeySequence.removeAt(previouslyUnhandledKeySequence.size - 1)
        mappingInfo = mapping.getLayer(previouslyUnhandledKeySequence)
      }
    }
    if (mappingInfo == null) {
      log.trace("Cannot find any mapping info for the sequence. Return false.")
      return false
    }
    processBuilder.addExecutionStep { b, c, d -> processCompleteMappingSequence(key, b, c, d, mappingInfo, currentMappingInfo) }
    return true
  }

  private fun processCompleteMappingSequence(
    key: KeyStroke,
    keyState: KeyHandlerState,
    editor: VimEditor,
    context: ExecutionContext,
    mappingInfo: MappingInfoLayer,
    currentMappingInfo: MappingInfoLayer?,
  ) {
    val mappingState = keyState.mappingState
    mappingState.resetMappingSequence()
    log.trace("Executing mapping info")
    try {
      mappingState.startMapExecution()
      mappingInfo.execute(editor, context, keyState)
    } catch (e: Exception) {
      injector.messages.showStatusBarMessage(editor, e.message)
      injector.messages.indicateError()
      log.error(
        """
                Caught exception during ${mappingInfo.getPresentableString()}
                ${e.message}
        """.trimIndent(),
        e
      )
    } catch (e: NotImplementedError) {
      injector.messages.showStatusBarMessage(editor, e.message)
      injector.messages.indicateError()
      log.error(
        """
                 Caught exception during ${mappingInfo.getPresentableString()}
                 ${e.message}
        """.trimIndent(),
        e
      )
    } finally {
      mappingState.stopMapExecution()
    }

    // If we've just evaluated the previous key sequence, make sure to also handle the current key
    if (mappingInfo !== currentMappingInfo) {
      log.trace("Evaluating the current key")
      KeyHandler.getInstance().handleKey(editor, key, context, allowKeyMappings = true, false, keyState)
    }
    log.trace("Success processing of mapping")
  }

  private fun handleAbandonedMappingSequence(processBuilder: KeyProcessResult.KeyProcessResultBuilder): Boolean {
    log.debug("Processing abandoned mapping sequence")
    // The user has terminated a mapping sequence with an unexpected key
    // E.g. if there is a mapping for "hello" and user enters command "help" the processing of "h", "e" and "l" will be
    //   prevented by this handler. Make sure the currently unhandled keys are processed as normal.
    val unhandledKeyStrokes = processBuilder.state.mappingState.detachKeys()

    // If there is only the current key to handle, do nothing
    if (unhandledKeyStrokes.size == 1) {
      log.trace("There is only one key in mapping. Return false.")
      return false
    }

    // Okay, look at the code below. Why is the first key handled separately?
    // Let's assume the next mappings:
    //   - map ds j
    //   - map I 2l
    // If user enters `dI`, the first `d` will be caught be this handler because it's a prefix for `ds` command.
    //  After the user enters `I`, the caught `d` should be processed without mapping, and the rest of keys
    //  should be processed with mappings (to make I work)
    processBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, lambdaContext ->
      processAbondonedMappingSequence(unhandledKeyStrokes, lambdaEditor, lambdaContext, lambdaKeyState) }
    return true
  }

  private fun processAbondonedMappingSequence(unhandledKeyStrokes: List<KeyStroke>, editor: VimEditor, context: ExecutionContext, keyState: KeyHandlerState) {
    if (isPluginMapping(unhandledKeyStrokes)) {
      log.trace("This is a plugin mapping, process it")
      KeyHandler.getInstance().handleKey(
        editor,
        unhandledKeyStrokes[unhandledKeyStrokes.size - 1],
        context,
        allowKeyMappings = true,
        mappingCompleted = false,
        keyState,
      )
    } else {
      log.trace("Process abandoned keys.")
      KeyHandler.getInstance()
        .handleKey(editor, unhandledKeyStrokes[0], context, allowKeyMappings = false, mappingCompleted = false, keyState)
      for (keyStroke in unhandledKeyStrokes.subList(1, unhandledKeyStrokes.size)) {
        KeyHandler.getInstance()
          .handleKey(editor, keyStroke, context, allowKeyMappings = true, mappingCompleted = false, keyState)
      }
    }
    log.trace("Return true from abandoned keys processing.")
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
}
