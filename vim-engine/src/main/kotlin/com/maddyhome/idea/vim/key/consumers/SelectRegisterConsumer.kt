/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key.consumers

import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.state.KeyHandlerState
import com.maddyhome.idea.vim.state.mode.Mode
import javax.swing.KeyStroke

internal class SelectRegisterConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<SelectRegisterConsumer>()
  }

  override fun isApplicable(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    return isSelectRegister(key, keyProcessResultBuilder.state)
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    logger.trace { "Entered SelectRegisterConsumer" }
    keyProcessResultBuilder.addExecutionStep { ks, _, _ ->
      ks.commandBuilder.startWaitingForRegister(key)
    }
    return true
  }

  private fun isSelectRegister(key: KeyStroke, keyState: KeyHandlerState): Boolean {
    val vimState = injector.vimState
    if (vimState.mode !is Mode.NORMAL && vimState.mode !is Mode.VISUAL) {
      return false
    }
    return keyState.commandBuilder.isRegisterPending || key.keyChar == '"'
  }
}
