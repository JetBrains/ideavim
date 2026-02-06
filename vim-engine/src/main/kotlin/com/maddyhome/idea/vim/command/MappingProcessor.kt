/*
 * Copyright 2003-2026 The IdeaVim authors
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

/**
 * Key consumer to apply mappings to the incoming keystroke(s) in all modes
 *
 * If mappings are allowed, keys are consumed and batched up until a matching map is completed or abandoned, by entering
 * a key that is not part of a mapping's right-hand side. If the `'timeout'` option is set, and a prefix is not matched
 * within `'timeoutlen'` milliseconds, the prefix is also abandoned.
 *
 * If a key sequence matches a map and doesn't have a longer sequence available, the key is consumed and the right-hand
 * side of the map is sent through the key handler. This recursive call to the key handler may or may not apply mappings
 * again, depending on how the mapping was declared (e.g. `nmap` vs `nnoremap`).
 *
 * When a prefix is abandoned, it is first checked for the next longest matching map. If found, the right-hand side is
 * sent through the key handler (recursively or non-recursively depending on the mapping), followed by the remaining
 * keys, with mapping enabled, as though the keys were directly typed. If not found, all the unhandled keys are
 * replayed, with the first key handled non-recursively to avoid cycles. The remaining keys are handled recursively.
 *
 * Escape (`<Esc>`/`<C-[>`) and cancel keys (`<C-C>`) are handled as part of a map's left-hand side. If these keys are
 * part of the current map sequence, they are consumed. If they don't, the map is abandoned and the keys are replayed
 * through the handler with the first in the sequence disallowing further mapping.
 */
internal object MappingProcessor : KeyConsumer {

  private val log = vimLogger<MappingProcessor>()

  override fun isApplicable(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    if (!allowKeyMappings) {
      log.trace("Mapping not allowed for key")
      return false
    }
    if (!isMappingApplicable(keyProcessResultBuilder.state.commandBuilder, key, keyProcessResultBuilder.state)) {
      log.trace { "Mapping not applicable for key: $key" }
      return false
    }
    return true
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    log.trace { "Entered MappingProcessor with key $key" }
    val keyState = keyProcessResultBuilder.state
    val mappingState = keyState.mappingState
    mappingState.stopMappingTimer()

    // Save the unhandled keystrokes until we either complete or abandon the sequence.
    log.trace { "Add key to mapping state: $key" }
    mappingState.addKey(key)

    val mappingMode = editor.mode.toMappingMode()
    log.trace { "Get keys for mapping mode. mode = $mappingMode" }
    val mapping = injector.keyGroup.getKeyMappingLayer(mappingMode)

    // Try to handle the key as part of an unfinished mapping sequence, completing a sequence or a key that terminates
    // an in-progress sequence
    val mappingProcessed =
      tryHandleUnfinishedMappingSequence(editor, keyProcessResultBuilder, mapping)
        || tryHandleCompletedMappingSequence(keyProcessResultBuilder, mapping)
        || tryHandleAbandonedMappingSequence(keyProcessResultBuilder)
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
  private fun tryHandleUnfinishedMappingSequence(
    editor: VimEditor,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
    mapping: KeyMappingLayer,
  ): Boolean {
    log.trace("Try processing unfinished mappings...")

    // If the current sequence, with the current key, is a prefix to one or more mappings, then it's unfinished. A
    // completed sequence is not a prefix to itself - this function will return false unless it's also a prefix for
    // other mappings.
    if (!mapping.isPrefix(
        keyProcessResultBuilder.state.mappingState.keys as? List<KeyStroke>
          ?: keyProcessResultBuilder.state.mappingState.keys.toList()
      )
    ) {
      log.debug("There are no mappings that start with the current sequence. Mapping processor will not handle further.")
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
      log.debug("Callback for 'timeout'. Replaying unhandled keys")

      // Note that there is an inherent but unlikely race condition here. If the timeout fires, it's possible that a key
      // is handled before invokeLater is called, which could reset the keys. All we can really do is check that the
      // unhandled keys list isn't empty at this point. If we detach before calling invokeLater, there might still be a
      // key handled, which means we're replaying (stale) keys after a key has been handled.
      // https://youtrack.jetbrains.com/issue/VIM-2392
      val unhandledKeys = keyState.mappingState.detachKeys()
      if (editor.isDisposed() || unhandledKeys.isEmpty()) {
        log.debug("Abandon mapping timer")
        return@invokeLater
      }

      replayUnhandledKeys(unhandledKeys, editor, context, keyState)
    }
  }

  /**
   * Handles a completed key sequence, if applicable
   *
   * If the current sequence completes a mapping, then execute it. If not, do nothing.
   */
  private fun tryHandleCompletedMappingSequence(
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
    mapping: KeyMappingLayer,
  ): Boolean {
    log.trace("Try processing complete mapping sequence...")

    val mappingState = keyProcessResultBuilder.state.mappingState
    val mappingInfo = mapping.getLayer(mappingState.keys as? List<KeyStroke> ?: mappingState.keys.toList())
    if (mappingInfo == null) {
      log.trace("Cannot find any mapping info for the sequence. Mapping processor will not handle further.")
      return false
    }

    keyProcessResultBuilder.addExecutionStep { ks, e, c ->
      log.trace("Executing mapping")

      val mappingState = ks.mappingState
      mappingState.resetMappingSequence()
      executeMappingInfo(mappingInfo, e, c, ks)

      log.trace("Completed mapping sequence processed. Returning true.")
    }
    return true
  }

  private fun executeMappingInfo(
    mappingInfo: MappingInfoLayer,
    editor: VimEditor,
    context: ExecutionContext,
    keyState: KeyHandlerState,
  ) {
    val mappingState = keyState.mappingState

    // Catch any exception, but also NotImplementedError. Don't just catch Throwable, as this will catch exceptions
    // thrown by log.error, which can include TestLoggerAssertionError
    try {
      mappingState.startMapExecution()
      mappingInfo.execute(editor, context, keyState)
    } catch (e: Exception) {
      injector.messages.showErrorMessage(editor, e.message)
      log.error(
        """
                  Caught exception during ${mappingInfo.getPresentableString()}
                  ${e.message}
          """.trimIndent(),
        e
      )
    } catch (e: NotImplementedError) {
      injector.messages.showErrorMessage(editor, e.message)
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
  }

  /**
   * Handle a key sequence that is no longer a prefix to a mapping
   *
   * If the user enters a key that is not part of a prefix and doesn't complete a mapping, then replay the unhandled
   * keys so that they get processed.
   */
  private fun tryHandleAbandonedMappingSequence(keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder): Boolean {
    log.debug("Trying to process abandoned mapping sequence...")

    // The user has terminated a mapping sequence with an unexpected key. Detach and replay the batched unhandled keys.
    val unhandledKeyStrokes = keyProcessResultBuilder.state.mappingState.detachKeys()

    // If there is only the current key to handle, return false. The mapping processor will not handle this, and the
    // other key consumers can have a go.
    if (unhandledKeyStrokes.size == 1) {
      log.trace("There is only one key in abandoned mapping sequence. Mapping processor will not handle further.")
      return false
    }

    keyProcessResultBuilder.addExecutionStep { ks, e, c ->
      log.trace("Replaying abandoned keys")
      replayUnhandledKeys(unhandledKeyStrokes, e, c, ks)
      log.trace("Abandoned keys processed. Returning true.")
    }
    return true
  }

  /**
   * Replays unhandled keys through the key handler
   *
   * If a key sequence is not a mapping, the keys we've batched up need to be replayed through the key handler and
   * properly processed.
   *
   * Firstly, we look for the next longest subsequence that is a mapping. If found, execute it and then replay the
   * remaining keys, with mapping enabled, as though they were typed.
   *
   * If there are no subsequences matching a mapping, replay all the keys. The first key should not allow mappings, or
   * we'll try to map the same prefix again, and fail again. Subsequent keys should allow mapping, also as though they
   * were typed.
   */
  private fun replayUnhandledKeys(
    unhandledKeys: List<KeyStroke>,
    editor: VimEditor,
    context: ExecutionContext,
    keyState: KeyHandlerState,
  ) {
    log.trace("Replaying unhandled keys. Looking for mapping in subsequence")
    val mappingLayer = injector.keyGroup.getKeyMappingLayer(editor.mode.toMappingMode())

    val subsequence = unhandledKeys.toMutableList()
    while (subsequence.isNotEmpty()) {
      val mappingInfo = mappingLayer.getLayer(subsequence)
      if (mappingInfo != null) {
        log.trace("Found mapping. Executing it and replaying the rest of the keys")

        executeMappingInfo(mappingInfo, editor, context, keyState)

        // Replay the rest of the keys, with mapping applied, as though they were typed
        unhandledKeys.subList(subsequence.size, unhandledKeys.size).forEach {
          KeyHandler.getInstance().handleKey(editor, it, context, allowKeyMappings = true, keyState)
        }
        return
      }

      subsequence.removeLast()
    }

    log.trace("Replaying unhandled keys. There is no mapping in subsequence. Replaying all keys")

    // There wasn't a complete sequence in the unhandled keys, so replay all of them. Do not allow mappings for the
    // first key, or we'll start to build the same prefix that just failed. Subsequent keys should allow mappings, as
    // though they were typed.
    val keyHandler = KeyHandler.getInstance()
    unhandledKeys.forEachIndexed { index, it ->
      keyHandler.handleKey(editor, it, context, allowKeyMappings = index != 0, keyState)
    }
  }
}
