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

public class ModeInputConsumer: KeyConsumer {
  private companion object {
    private val logger = vimLogger<ModeInputConsumer>()
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    mappingCompleted: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
    shouldRecord: KeyHandler.MutableBoolean,
  ): Boolean {
    logger.trace { "Entered ModeInputConsumer" }
    val isProcessed = when (editor.mode) {
      Mode.INSERT, Mode.REPLACE -> {
        logger.trace("Process insert or replace")
        val keyProcessed = injector.changeGroup.processKey(editor, key, keyProcessResultBuilder)
        shouldRecord.value = keyProcessed && shouldRecord.value
        keyProcessed
      }
      is Mode.SELECT -> {
        logger.trace("Process select")
        val keyProcessed = injector.changeGroup.processKeyInSelectMode(editor, key, keyProcessResultBuilder)
        shouldRecord.value = keyProcessed && shouldRecord.value
        keyProcessed
      }
      is Mode.CMD_LINE -> {
        logger.trace("Process cmd line")
        val keyProcessed = injector.processGroup.processExKey(editor, key, keyProcessResultBuilder)
        shouldRecord.value = keyProcessed && shouldRecord.value
        keyProcessed
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