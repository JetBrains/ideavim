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
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.state.mode.Mode
import javax.swing.KeyStroke

internal class ModeInputConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<ModeInputConsumer>()
  }

  override fun isApplicable(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    return true
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    logger.trace { "Entered ModeInputConsumer" }
    val isProcessed = when (editor.mode) {
      Mode.INSERT, Mode.REPLACE -> {
        logger.trace("Process insert or replace")
        val keyProcessed = injector.changeGroup.processKey(editor, key, keyProcessResultBuilder)
        keyProcessed
      }

      is Mode.SELECT -> {
        logger.trace("Process select")
        val keyProcessed = injector.changeGroup.processKeyInSelectMode(editor, key, keyProcessResultBuilder)
        keyProcessed
      }

      is Mode.CMD_LINE -> {
        val commandLine = injector.commandLine.getActiveCommandLine()
        if (commandLine != null) {
          keyProcessResultBuilder.addExecutionStep { _, _, _ ->
            commandLine.focus()
            commandLine.handleKey(key)
          }
        } else {
          keyProcessResultBuilder.addExecutionStep { _, lambdaEditor, _ ->
            lambdaEditor.mode = Mode.NORMAL()
            KeyHandler.getInstance().reset(lambdaEditor)
          }
        }
        true
      }

      else -> {
        false
      }
    }
    if (isProcessed) {
      keyProcessResultBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, _ ->
        lambdaKeyState.partialReset(lambdaEditor.mode)
      }
    }
    return isProcessed
  }
}
