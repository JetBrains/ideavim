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
import com.maddyhome.idea.vim.key.getLayer
import com.maddyhome.idea.vim.key.isPrefix
import com.maddyhome.idea.vim.state.KeyHandlerState
import javax.swing.KeyStroke

object MappingProcessor : KeyConsumer {

  private val log = vimLogger<MappingProcessor>()

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    mappingCompleted: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    log.trace { "Entered MappingProcessor with key $key" }

    if (!allowKeyMappings) return false

    log.debug("Start processing key mappings.")

    val keyState = keyProcessResultBuilder.state

    if (!isMappingApplicable(keyState.commandBuilder, key, keyState)) {
      log.debug("Mapping not applicable. Finish key processing, returning false")
      return false
    }

    val mappingState = keyState.mappingState
    mappingState.stopMappingTimer()

    // Save the unhandled keystrokes until we either complete or abandon the sequence.
    log.trace { "Add key to mapping state: $key" }
    mappingState.addKey(key)

    val mappingMode = editor.mode.toMappingMode()
    log.trace { "Get keys for mapping mode. mode = $mappingMode" }
    val mapping = injector.keyGroup.getKeyMappingLayer(mappingMode)

    // Returns true if any of these methods handle the key. False means that the key is unrelated to mapping and should
    // be processed as normal.
    // TODO: This pipeline is confusing - each function has knowledge of where it is in the pipeline
    // E.g. handleCompleteMappingSequence assumes it's not a prefix because it's called after handleUnfinishedMappingSeq
    // Perhaps this should be simplified to `if (isUnfinishedMappingSequence) process() else if (isComplete...)`?
    // The benefit of the existing functions is that we always fall through to handleAbandoned, which sorts everything
    val mappingProcessed =
      handleUnfinishedMappingSequence(editor, keyProcessResultBuilder, mapping, mappingCompleted)
        || handleCompleteMappingSequence(keyProcessResultBuilder, mapping, key)
        || handleAbandonedMappingSequence(keyProcessResultBuilder)
    log.debug { "Finish mapping processing. Return $mappingProcessed" }

    return mappingProcessed
  }

  private fun isMappingApplicable(commandBuilder: CommandBuilder, key: KeyStroke, keyState: KeyHandlerState): Boolean {
    // Mapping is not applied to character/digraph arguments (e.g. `f{char}` or register names).
    // It's also not applied partway through an existing command - e.g. `<C-W>s` does not apply any maps for `s`.
    return !commandBuilder.isAwaitingCharOrDigraphArgument()
      && !commandBuilder.isBuildingMultiKeyCommand()
      && !commandBuilder.isRegisterPending
      && !isTypingZeroInCommandCount(key, keyState)
  }

  private fun isTypingZeroInCommandCount(key: KeyStroke, keyState: KeyHandlerState): Boolean {
    // "0" can be mapped, but the mapping isn't applied when entering a count. Other digits are always mapped, even when
    // entering a count. Note that the docs state that this is for Normal, but it also applies in Visual and Op-pending.
    // (E.g. `:imap 0 3` -> `d20w` will still delete 20 words, not 23, but `d0w` will delete 3, `2d0w` will delete 6).
    // Essentially, if we've got a count, don't map.
    // See `:help :map-modes`
    return key.keyChar == '0' && keyState.commandBuilder.hasCountCharacters()
  }

  /**
   * Handles the next key in an ongoing key sequence, if applicable
   *
   * If the current key sequence, including the current key, is a prefix to other mappings, then we consider this an
   * unfinished sequence. We wait for the next keystroke or, if `'timeout'` is set, start a timer to abandon the mapping
   * if no other keys are pressed.
   */
  private fun handleUnfinishedMappingSequence(
    editor: VimEditor,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
    mapping: KeyMappingLayer,
    mappingCompleted: Boolean,
  ): Boolean {
    log.trace("Processing unfinished mappings...")

    // This is set when we need to replay an unhandled key sequence. That sequence was a prefix to a mapping that wasn't
    // completed, either due to a timeout or a key that wasn't part of the mapping. The unhandled keys are passed
    // through the key handler again, with this flag set to prevent us trying to map them again.
    // TODO: Try to remove this, so we don't have to pass yet more state around
    // When replaying, we should find the longest viable mapping and invoke it, then pass the remaining keys back to key
    // handler without any flags set - they can be mapped. If there is no mapping in the previous sequence, then pass
    // all the keys back to key handler with the allowKeyMappings flag set to false.
    if (mappingCompleted) {
      log.trace("Mapping is already completed. Returning false.")
      return false
    }

    // If the current sequence, with the current key, is a prefix to one or more mappings, then it's unfinished. A
    // completed sequence is not a prefix to itself - this function will return false unless it's also a prefix for
    // other mappings.
    if (!mapping.isPrefix(keyProcessResultBuilder.state.mappingState.keys)) {
      log.debug("There are no mappings that start with the current sequence. Returning false.")
      return false
    }

    // If the 'timeout' option is set, start a timer that will abandon the sequence and replay the unhandled keys
    // unmapped. Every time a key is pressed and handled, the timer is stopped. E.g. if there is a mapping for "dweri",
    // and the user has typed "dw" wait for the timeout, and then replay "d" and "w" without any mapping (which will of
    // course delete a word)
    if (injector.options(editor).timeout) {
      log.trace("'timeout' is set. Scheduling the mapping timer")

      keyProcessResultBuilder.addExecutionStep { ks, e, c ->
        ks.mappingState.startMappingTimer { onUnfinishedMappingSequenceTimeout(editor, ks, c) }
      }
    }
    else {
      log.trace("'timeout' is not set. Waiting for the next keystroke")
    }
    return true
  }

  private fun onUnfinishedMappingSequenceTimeout(
    editor: VimEditor,
    keyState: KeyHandlerState,
    context: ExecutionContext,
  ) {
      injector.application.invokeLater(editor) {
        log.debug("Delayed mapping timer call")

        val unhandledKeys = keyState.mappingState.detachKeys()

        // XXX There is a strange issue that reports that mapping state is empty at the moment of the function call.
        //   At the moment, I see the only one possibility this to happen - other key is handled after the timer executed,
        //   but before invoke later is handled. This is a rare case, so I'll just add a check to isPluginMapping.
        //   But this "unexpected behaviour" exists, and it would be better not to relay on mutable state with delays.
        //   https://youtrack.jetbrains.com/issue/VIM-2392
        if (editor.isDisposed() || isPluginMapping(unhandledKeys)) {
          log.debug("Abandon mapping timer")
          return@invokeLater
        }

        log.trace("Replaying unhandled keys...")

        // TODO: Centralise replaying unhandled keys
        // Find the longest sequence that's a mapping and execute it. Then replay the remaining keys, allowing
        // mapping. This will remove the need for mappingComplete.
        for ((index, keyStroke) in unhandledKeys.withIndex()) {
          // TODO: There is a bug if the previous longest sequence is not the last char
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
            context,
            allowKeyMappings = true,
            mappingCompleted = lastKeyInSequence,
            keyState,
          )
        }
      }
  }

  /**
   * Handles a complete key sequence, if applicable
   *
   * If the current sequence completes a mapping, then execute it. If not, do nothing.
   *
   * TODO: This documentation doesn't match current implementation.
   */
  private fun handleCompleteMappingSequence(
    processBuilder: KeyProcessResult.KeyProcessResultBuilder,
    mapping: KeyMappingLayer,
    key: KeyStroke,
  ): Boolean {
    log.trace("Processing complete mapping sequence...")

    // The current sequence isn't a prefix, check to see if it's a completed sequence.
    // TODO: The current sequence might be a prefix
    // If an unfinished sequence times out, we replay with mapping enabled, and set mappingComplete for the last char,
    // which skips the unfinished check. In this case, it is a prefix, but we try to complete it, or complete a mapping
    // for the sequence that ends the character before.
    // If we simplify the replay mechanism, we don't need to worry about this.
    val mappingState = processBuilder.state.mappingState
    val mappingInfoForCurrentSequence = mapping.getLayer(mappingState.keys)
    var mappingInfo = mappingInfoForCurrentSequence
    if (mappingInfo == null) {
      log.trace("Haven't found any mapping info for the given sequence. Trying to apply mapping to a subsequence.")

      // It's an abandoned sequence, check to see if the previous sequence was a complete sequence.
      // TODO: This is incorrect behaviour
      // What about sequences that were completed N keys ago?
      // I.e. if we have `imap ab AB` and `imap abcde ABCDE`, this will try to handle `abcd`, which isn't a map
      // This will likely drop us into handleAbandonedMappingSequence, which will start replaying key strokes, but
      // crucially without mapping and without invoking any earlier complete sequences, so we end up with `abcd` instead
      // of `ABcd`.
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

    processBuilder.addExecutionStep { ks, e, c ->
      processCompleteMappingSequence(key, ks, e, c, mappingInfo, mappingInfoForCurrentSequence)
    }
    return true
  }

  private fun processCompleteMappingSequence(
    key: KeyStroke,
    keyState: KeyHandlerState,
    editor: VimEditor,
    context: ExecutionContext,
    mappingInfo: MappingInfoLayer,
    mappingInfoForCurrentSequence: MappingInfoLayer?,
  ) {
    val mappingState = keyState.mappingState
    mappingState.resetMappingSequence()

    log.trace("Executing mapping info")

    // Catch any exception, but also NotImplementedError. Don't just catch Throwable, as this will catch exceptions
    // thrown by log.error, which can include TestLoggerAssertionError
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
    if (mappingInfo !== mappingInfoForCurrentSequence) {
      log.trace("Evaluating the current key")
      KeyHandler.getInstance().handleKey(editor, key, context, allowKeyMappings = true, false, keyState)
    }
    log.trace("Completed mapping sequence")
  }

  /**
   * Handle a key sequence that is no longer a prefix to a mapping
   *
   * If the user enters a key that is not part of a prefix and doesn't complete a mapping, then replay the unhandled
   * keys so that they get processed.
   */
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

    processBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, lambdaContext ->
      processAbandonedMappingSequence(unhandledKeyStrokes, lambdaEditor, lambdaContext, lambdaKeyState)
    }
    return true
  }

  private fun processAbandonedMappingSequence(
    unhandledKeyStrokes: List<KeyStroke>,
    editor: VimEditor,
    context: ExecutionContext,
    keyState: KeyHandlerState,
  ) {
    if (isPluginMapping(unhandledKeyStrokes)) {
      log.trace("This is a plugin mapping, process it")

      // We've typed a key that causes us to abandon the sequence. If it's an (unfinished) <Plug> sequence, ignore the
      // plugin sequence and only replay the last character?
      // TODO: Why do we skip this <Plug> prefix? Why not push it through without mapping? Surely that's the expected behaviour
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

      // Okay, look at the code below. Why is the first key handled separately?
      // Let's assume the next mappings:
      //   - map ds j
      //   - map I 2l
      // If user enters `dI`, the first `d` will be caught be this handler because it's a prefix for `ds` command.
      //  After the user enters `I`, the caught `d` should be processed without mapping, and the rest of keys
      //  should be processed with mappings (to make I work)
      KeyHandler.getInstance()
        .handleKey(
          editor,
          unhandledKeyStrokes[0],
          context,
          allowKeyMappings = false,
          mappingCompleted = false,
          keyState
        )
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
  // TODO: If we change how we replay keys, this logic needs to be revisited
  // User types `I`, which is a complete sequence. Since it's a recursive mapping, `<Plug>i` is pushed through the key
  // handler. It's an unfinished sequence. The user types `b` and we've now got an abandoned sequence. I would expect
  // Vim to replay everything (`<Plug>ib`) with no mapping, but we seem to skip the `<Plug>` prefix completely.
  // I still don't understand why
  private fun isPluginMapping(unhandledKeyStrokes: List<KeyStroke>): Boolean {
    return unhandledKeyStrokes.isNotEmpty() && unhandledKeyStrokes[0] == injector.parser.plugKeyStroke
  }
}
