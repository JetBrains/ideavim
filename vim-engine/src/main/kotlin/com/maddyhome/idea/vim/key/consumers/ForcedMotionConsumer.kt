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
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.key.KeySource
import com.maddyhome.idea.vim.state.mode.Mode
import javax.swing.KeyStroke

/**
 * Handles the `v` modifier entered between an operator and its motion (`:help o_v`), e.g. `dvw`.
 *
 * While an operator is pending, `v` forces the following motion to be characterwise. Rather than becoming the motion
 * argument itself, it stashes a forced motion type on the [com.maddyhome.idea.vim.command.CommandBuilder], which is
 * later applied to the motion argument. This mirrors Neovim's `nv_visual`, which records `motion_force` and keeps the
 * operator pending instead of finishing it.
 */
class ForcedMotionConsumer : KeyConsumer {

  private  val chars: Map<Char, MotionType> = mapOf(
    'v' to MotionType.INCLUSIVE,
    'V' to MotionType.LINE_WISE,
    '\u000C' to MotionType.LINE_WISE,
  )

  override fun isApplicable(
    key: KeyStroke,
    editor: VimEditor,
    keySource: KeySource,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    return editor.mode is Mode.OP_PENDING && chars.keys.contains(key.keyChar)
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    keySource: KeySource,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    keyProcessResultBuilder.state.commandBuilder.forcedMotion = chars[key.keyChar]
    return true
  }
}