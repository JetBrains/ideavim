/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key.consumers

import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.key.KeySource
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * Handles the `v`, `V` and `CTRL-V` modifiers entered between an operator and its motion (`:help o_v`), e.g. `dvw`.
 *
 * While an operator is pending, these keys force the type of the following motion - characterwise, linewise or
 * blockwise respectively. Rather than becoming the motion argument itself, the forced type is stashed on the
 * [com.maddyhome.idea.vim.command.CommandBuilder] and later applied to the motion argument. This mirrors Neovim's
 * `nv_visual`, which records `motion_force` and keeps the operator pending instead of finishing it.
 */
class ForcedMotionConsumer : KeyConsumer {

  override fun isApplicable(
    key: KeyStroke,
    editor: VimEditor,
    keySource: KeySource,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    if (keyProcessResultBuilder.state.commandBuilder.isAwaitingCharacterBasedArgument()) return false
    return editor.mode is Mode.OP_PENDING && forcedTypeFor(key) != null
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    keySource: KeySource,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    keyProcessResultBuilder.state.commandBuilder.forcedMotion = forcedTypeFor(key)
    return true
  }

  /**
   * Maps a keystroke to the motion type it forces: `v` -> characterwise, `V` -> linewise, `CTRL-V`/`CTRL-Q` ->
   * blockwise. Returns `null` for any other key.
   */
  private fun forcedTypeFor(key: KeyStroke): SelectionType? {
    if (key.modifiers and InputEvent.CTRL_DOWN_MASK != 0) {
      // CTRL-V and its alias CTRL-Q force blockwise
      return if (key.keyCode == KeyEvent.VK_V || key.keyCode == KeyEvent.VK_Q) SelectionType.BLOCK_WISE else null
    }
    return when (key.keyChar) {
      'v' -> SelectionType.CHARACTER_WISE
      'V' -> SelectionType.LINE_WISE
      else -> null
    }
  }
}
