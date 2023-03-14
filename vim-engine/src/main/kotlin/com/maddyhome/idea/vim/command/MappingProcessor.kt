/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.key.KeyMappingLayer
import javax.swing.KeyStroke

public object MappingProcessor {

  private val log = vimLogger<MappingProcessor>()

  internal fun handleKeyMapping(
    editor: VimEditor,
    key: KeyStroke,
    context: ExecutionContext,
    mappingCompleted: Boolean,
  ): Boolean {
    log.debug("Start processing key mappings.")
    val commandState = editor.vimStateMachine
    val mappingState = commandState.mappingState
    val commandBuilder = commandState.commandBuilder
    if (commandBuilder.isAwaitingCharOrDigraphArgument() ||
      commandBuilder.isBuildingMultiKeyCommand() ||
      isMappingDisabledForKey(key, commandState) ||
      commandState.isRegisterPending
    ) {
      log.debug("Finish key processing, returning false")
      return false
    }
    mappingState.stopMappingTimer()

    // Save the unhandled keystrokes until we either complete or abandon the sequence.
    log.trace("Add key to mapping state")
    mappingState.addKey(key)
    val mapping = injector.keyGroup.getKeyMappingLayer(mappingState.mappingMode)
    log.trace { "Get keys for mapping mode. mode = " + mappingState.mappingMode }

    // Returns true if any of these methods handle the key. False means that the key is unrelated to mapping and should
    // be processed as normal.
    val mappingProcessed =
      handleUnfinishedMappingSequence(editor, mappingState, mapping, mappingCompleted) ||
        handleCompleteMappingSequence(editor, context, mappingState, mapping, key) ||
        handleAbandonedMappingSequence(editor, mappingState, context)
    log.debug { "Finish mapping processing. Return $mappingProcessed" }

    return mappingProcessed
  }

  private fun isMappingDisabledForKey(key: KeyStroke, vimStateMachine: VimStateMachine): Boolean {
    // "0" can be mapped, but the mapping isn't applied when entering a count. Other digits are always mapped, even when
    // entering a count.
    // See `:help :map-modes`
    val isMappingDisabled = key.keyChar == '0' && vimStateMachine.commandBuilder.count > 0
    log.debug { "Mapping disabled for key: $isMappingDisabled" }
    return isMappingDisabled
  }

  private fun handleUnfinishedMappingSequence(
    editor: VimEditor,
    mappingState: MappingState,
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
    if (!mapping.isPrefix(mappingState.keys)) {
      log.debug("There are no mappings that start with the current sequence. Returning false.")
      return false
    }

    // If the timeout option is set, set a timer that will abandon the sequence and replay the unhandled keys unmapped.
    // Every time a key is pressed and handled, the timer is stopped. E.g. if there is a mapping for "dweri", and the
    // user has typed "dw" wait for the timeout, and then replay "d" and "w" without any mapping (which will of course
    // delete a word)
    if (injector.options(editor).isSet(Options.timeout)
    ) {
      log.trace("timeout is set. schedule a mapping timer")
      // XXX There is a strange issue that reports that mapping state is empty at the moment of the function call.
      //   At the moment, I see the only one possibility this to happen - other key is handled after the timer executed,
      //   but before invoke later is handled. This is a rare case, so I'll just add a check to isPluginMapping.
      //   But this "unexpected behaviour" exists, and it would be better not to relay on mutable state with delays.
      //   https://youtrack.jetbrains.com/issue/VIM-2392
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

              KeyHandler.getInstance().handleKey(
                editor,
                keyStroke,
                injector.executionContextManager.onEditor(editor),
                allowKeyMappings = true,
                mappingCompleted = lastKeyInSequence,
              )
            }
          },
          editor,
        )
      }
    }
    log.trace("Unfinished mapping processing finished")
    return true
  }

  private fun handleCompleteMappingSequence(
    editor: VimEditor,
    context: ExecutionContext,
    mappingState: MappingState,
    mapping: KeyMappingLayer,
    key: KeyStroke,
  ): Boolean {
    log.trace("Processing complete mapping sequence...")
    // The current sequence isn't a prefix, check to see if it's a completed sequence.
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
    mappingState.resetMappingSequence()
    val currentContext = context.updateEditor(editor)
    log.trace("Executing mapping info")
    try {
      mappingState.startMapExecution()
      mappingInfo.execute(editor, context)
    } catch (e: Exception) {
      injector.messages.showStatusBarMessage(editor, e.message)
      injector.messages.indicateError()
      log.warn(
        """
                Caught exception during ${mappingInfo.getPresentableString()}
                ${e.message}
        """.trimIndent(),
      )
    } catch (e: NotImplementedError) {
      injector.messages.showStatusBarMessage(editor, e.message)
      injector.messages.indicateError()
      log.warn(
        """
                 Caught exception during ${mappingInfo.getPresentableString()}
                 ${e.message}
        """.trimIndent(),
      )
    } finally {
      mappingState.stopMapExecution()
    }

    // If we've just evaluated the previous key sequence, make sure to also handle the current key
    if (mappingInfo !== currentMappingInfo) {
      log.trace("Evaluating the current key")
      KeyHandler.getInstance().handleKey(editor, key, currentContext, allowKeyMappings = true, false)
    }
    log.trace("Success processing of mapping")
    return true
  }

  private fun handleAbandonedMappingSequence(
    editor: VimEditor,
    mappingState: MappingState,
    context: ExecutionContext,
  ): Boolean {
    log.debug("Processing abandoned mapping sequence")
    // The user has terminated a mapping sequence with an unexpected key
    // E.g. if there is a mapping for "hello" and user enters command "help" the processing of "h", "e" and "l" will be
    //   prevented by this handler. Make sure the currently unhandled keys are processed as normal.
    val unhandledKeyStrokes = mappingState.detachKeys()

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
    if (isPluginMapping(unhandledKeyStrokes)) {
      log.trace("This is a plugin mapping, process it")
      KeyHandler.getInstance().handleKey(
        editor,
        unhandledKeyStrokes[unhandledKeyStrokes.size - 1],
        context,
        allowKeyMappings = true,
        mappingCompleted = false,
      )
    } else {
      log.trace("Process abandoned keys.")
      KeyHandler.getInstance()
        .handleKey(editor, unhandledKeyStrokes[0], context, allowKeyMappings = false, mappingCompleted = false)
      for (keyStroke in unhandledKeyStrokes.subList(1, unhandledKeyStrokes.size)) {
        KeyHandler.getInstance()
          .handleKey(editor, keyStroke, context, allowKeyMappings = true, mappingCompleted = false)
      }
    }
    log.trace("Return true from abandoned keys processing.")
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
}
