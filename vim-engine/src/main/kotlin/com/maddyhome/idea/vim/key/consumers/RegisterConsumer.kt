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
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class RegisterConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<CharArgumentConsumer>()
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    mappingCompleted: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
    shouldRecord: KeyHandler.MutableBoolean,
  ): Boolean {
    logger.trace { "Entered RegisterConsumer" }
    if (!injector.vimState.isRegisterPending) return false

    logger.trace("Pending mode.")
    keyProcessResultBuilder.state.commandBuilder.addKey(key)

    val chKey: Char = if (key.keyChar == KeyEvent.CHAR_UNDEFINED) 0.toChar() else key.keyChar
    handleSelectRegister(editor, chKey, keyProcessResultBuilder)
    return true
  }

  private fun handleSelectRegister(editor: VimEditor, chKey: Char, processBuilder: KeyProcessResult.KeyProcessResultBuilder) {
    logger.trace("Handle select register")
    injector.vimState.resetRegisterPending()
    if (injector.registerGroup.isValid(chKey)) {
      logger.trace("Valid register")
      processBuilder.state.commandBuilder.pushCommandPart(chKey)
    } else {
      processBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, _ ->
        logger.trace("Invalid register, set command state to BAD_COMMAND")
        KeyHandler.getInstance().setBadCommand(lambdaEditor, lambdaKeyState)
      }
    }
  }
}