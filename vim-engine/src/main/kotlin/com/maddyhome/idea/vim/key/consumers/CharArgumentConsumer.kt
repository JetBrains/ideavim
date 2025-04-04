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
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.CommandBuilder
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.key.KeyConsumer
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

internal class CharArgumentConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<CharArgumentConsumer>()
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    logger.trace { "Entered CharArgumentConsumer" }
    if (!isExpectingCharArgument(keyProcessResultBuilder.state.commandBuilder)) return false

    val chKey: Char = if (key.keyChar == KeyEvent.CHAR_UNDEFINED) 0.toChar() else key.keyChar
    handleCharArgument(key, chKey, keyProcessResultBuilder)
    return true
  }

  private fun isExpectingCharArgument(commandBuilder: CommandBuilder): Boolean {
    val expectingCharArgument = commandBuilder.expectedArgumentType === Argument.Type.CHARACTER
    logger.debug { "Expecting char argument: $expectingCharArgument" }
    return expectingCharArgument
  }

  private fun handleCharArgument(
    key: KeyStroke,
    chKey: Char,
    processBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ) {
    var mutableChKey = chKey
    logger.trace("Handling char argument")
    // We are expecting a character argument - is this a regular character the user typed?
    // Some special keys can be handled as character arguments - let's check for them here.
    if (mutableChKey.code == 0) {
      when (key.keyCode) {
        KeyEvent.VK_TAB -> mutableChKey = '\t'
        KeyEvent.VK_ENTER -> mutableChKey = '\n'
      }
    }
    val commandBuilder = processBuilder.state.commandBuilder
    if (mutableChKey.code != 0) {
      processBuilder.addExecutionStep { _, lambdaEditor, _ ->
        // Create the character argument, add it to the current command, and signal we are ready to process the command
        logger.trace("Add character argument to the current command")
        commandBuilder.addArgument(Argument.Character(mutableChKey))
        lambdaEditor.isReplaceCharacter = false
      }
    } else {
      logger.trace("This is not a valid character argument. Set command state to BAD_COMMAND")
      // Oops - this isn't a valid character argument
      processBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, _ ->
        KeyHandler.getInstance().setBadCommand(lambdaEditor, lambdaKeyState)
      }
    }
  }
}
