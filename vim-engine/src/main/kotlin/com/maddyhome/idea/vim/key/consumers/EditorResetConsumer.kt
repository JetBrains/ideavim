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
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.isCloseKeyStroke
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.state.KeyHandlerState
import com.maddyhome.idea.vim.state.mode.Mode
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Key consumer to handle escape and cancel keys in Normal mode
 *
 * This consumer will also pass `<Esc>` to the IDE so it can e.g. remove highlights, etc.
 *
 * At least some of this implementation can be moved to command handlers.
 *
 * This consumer handles escape and cancel keys, but only for Normal mode, and with no distinction between escape or
 * cancel.
 *
 * TODO: What about Visual mode? I think this leaves a hole in escape processing for Visual
 * E.g. `"<Esc>` will reset the key handler, but `v"<Esc>` will not. It will go through [SelectRegisterConsumer] and
 * error, resetting the key handler, but also beeping
 */
internal class EditorResetConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<EditorResetConsumer>()
  }

  override fun isApplicable(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    val editorReset = editor.mode is Mode.NORMAL && key.isCloseKeyStroke()
    logger.debug { "This is editor reset: $editorReset" }
    return editorReset
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    logger.trace { "Entered EditorResetConsumer" }
    keyProcessResultBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, lambdaContext ->
      handleEditorReset(
        lambdaEditor,
        key,
        lambdaKeyState,
        lambdaContext
      )
    }
    return true
  }

  private fun handleEditorReset(
    editor: VimEditor,
    key: KeyStroke,
    keyState: KeyHandlerState,
    context: ExecutionContext,
  ) {
    val commandBuilder = keyState.commandBuilder
    if (commandBuilder.isAwaitingCharOrDigraphArgument()) {
      editor.isReplaceCharacter = false
    }
    if (commandBuilder.isEmpty) {
      val register = injector.registerGroup
      if (register.currentRegister == register.defaultRegister) {
        // Escape should exit "Insert Normal" mode. We don't have a handler for <Esc> in Normal mode, so we do it here
        val mode = editor.mode
        if (mode is Mode.NORMAL && (mode.isInsertPending || mode.isReplacePending)) {
          editor.mode = mode.returnTo
        } else {
          var indicateError = true
          if (key.keyCode == KeyEvent.VK_ESCAPE) {
            val executed = arrayOf<Boolean?>(null)
            injector.actionExecutor.executeCommand(
              editor,
              { executed[0] = injector.actionExecutor.executeEsc(editor, context) },
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
    }
    KeyHandler.getInstance().reset(keyState, editor.mode)

    // Make sure the caret attributes are up to date. If we've just changed the mode, then they will be. But if someone
    // has changed the caret while we weren't expecting, then this will at least sync the caret when hitting Escape.
    // This is a small workaround for an issue in the initial implementation of Next Edit Suggestions. It changes the
    // caret colour when it has a suggestion, capturing the original caret attributes when it does so. When it resets,
    // it restores the cached attributes. Unfortunately, we might easily have changed mode between times. I've seen it
    // happen where NES manages to capture the Op-pending heavy underscore caret attributes, and it's very confusing
    // when they're restored (LLM-19241).
    // Ideally, NES would modify the current caret attributes' colour, rather than recreate and/or cache. If this
    // doesn't get fixed by NES, we could reset caret attributes after every command, which should reduce how long the
    // incorrect caret is shown.
    injector.editorGroup.updateCaretsVisualAttributes(editor)
  }
}
