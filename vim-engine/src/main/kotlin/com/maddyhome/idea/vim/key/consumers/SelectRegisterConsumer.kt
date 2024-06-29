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
import com.maddyhome.idea.vim.state.KeyHandlerState
import com.maddyhome.idea.vim.state.VimStateMachine
import com.maddyhome.idea.vim.state.mode.Mode
import javax.swing.KeyStroke

class SelectRegisterConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<SelectRegisterConsumer>()
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    mappingCompleted: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
    shouldRecord: KeyHandler.MutableBoolean,
  ): Boolean {
    logger.trace { "Entered SelectRegisterConsumer" }
    val state = keyProcessResultBuilder.state
    if (!isSelectRegister(key, state)) return false

    logger.trace("Select register")
    state.commandBuilder.addKey(key)
    keyProcessResultBuilder.addExecutionStep { _, lambdaEditor, _ ->
      injector.vimState.isRegisterPending = true
    }
    return true
  }

  private fun isSelectRegister(key: KeyStroke, keyState: KeyHandlerState): Boolean {
    val vimState = injector.vimState
    if (vimState.mode !is Mode.NORMAL && vimState.mode !is Mode.VISUAL) {
      return false
    }
    return if (vimState.isRegisterPending) {
      true
    } else {
      key.keyChar == '"' && !KeyHandler.getInstance().isOperatorPending(vimState.mode, keyState) && keyState.commandBuilder.expectedArgumentType == null
    }
  }
}