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
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.state.KeyHandlerState
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.key.VimKeyStroke
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_DELETE

internal class DeleteCommandConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<DeleteCommandConsumer>()
  }

  override fun consumeKey(
    key: VimKeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    logger.trace { "Entered DeleteCommandConsumer" }
    if (!isDeleteCommandCountKey(key, keyProcessResultBuilder.state, editor.mode)) return false
    keyProcessResultBuilder.state.commandBuilder.deleteCountCharacter()
    return true
  }

  private fun isDeleteCommandCountKey(key: VimKeyStroke, keyState: KeyHandlerState, mode: Mode): Boolean {
    // See `:help N<Del>`
    if (key.keyCode != VK_DELETE) {
      logger.debug("Not a delete key")
      return false
    }

    val commandBuilder = keyState.commandBuilder
    if ((mode is Mode.NORMAL || mode is Mode.VISUAL || mode is Mode.OP_PENDING)
      && commandBuilder.isExpectingCount && commandBuilder.hasCountCharacters()
    ) {
      logger.debug("Deleting a count character")
      return true
    }

    logger.debug("Not a delete key")
    return false
  }
}
