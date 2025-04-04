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

internal class RegisterConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<CharArgumentConsumer>()
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    logger.trace { "Entered RegisterConsumer" }
    val commandBuilder = keyProcessResultBuilder.state.commandBuilder
    if (!commandBuilder.isRegisterPending) return false

    logger.trace("Pending mode.")
    commandBuilder.addTypedKeyStroke(key)

    val chKey: Char = if (key.keyChar == KeyEvent.CHAR_UNDEFINED) 0.toChar() else key.keyChar
    handleSelectRegister(chKey, keyProcessResultBuilder)
    return true
  }

  private fun handleSelectRegister(chKey: Char, processBuilder: KeyProcessResult.KeyProcessResultBuilder) {
    logger.trace("Handle select register")
    if (injector.registerGroup.isValid(chKey)) {
      logger.trace("Valid register")
      processBuilder.state.commandBuilder.selectRegister(chKey)
    } else {
      processBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, _ ->
        logger.trace("Invalid register, set command state to BAD_COMMAND")
        KeyHandler.getInstance().setBadCommand(lambdaEditor, lambdaKeyState)
      }
    }
  }
}
