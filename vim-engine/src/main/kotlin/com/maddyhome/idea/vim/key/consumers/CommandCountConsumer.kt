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
import com.maddyhome.idea.vim.state.mode.Mode
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class CommandCountConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<CommandCountConsumer>()
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    mappingCompleted: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
    shouldRecord: KeyHandler.MutableBoolean,
  ): Boolean {
    logger.trace { "Entered CommandCountConsumer" }
    val chKey: Char = if (key.keyChar == KeyEvent.CHAR_UNDEFINED) 0.toChar() else key.keyChar
    if (!isCommandCountKey(chKey, keyProcessResultBuilder.state, editor)) return false

    keyProcessResultBuilder.state.commandBuilder.addCountCharacter(key)
    return true
  }

  private fun isCommandCountKey(chKey: Char, keyState: KeyHandlerState, editor: VimEditor): Boolean {
    // Make sure to avoid handling '0' as the start of a count.
    val editorState = injector.vimState
    val commandBuilder = keyState.commandBuilder
    val notRegisterPendingCommand = editorState.mode is Mode.NORMAL && !editorState.isRegisterPending
    val visualMode = editorState.mode is Mode.VISUAL && !editorState.isRegisterPending
    val opPendingMode = editorState.mode is Mode.OP_PENDING

    if (notRegisterPendingCommand || visualMode || opPendingMode) {
      if (commandBuilder.isExpectingCount && Character.isDigit(chKey) && (commandBuilder.count > 0 || chKey != '0')) {
        logger.debug("This is a command key count")
        return true
      }
    }
    logger.debug("This is NOT a command key count")
    return false
  }
}