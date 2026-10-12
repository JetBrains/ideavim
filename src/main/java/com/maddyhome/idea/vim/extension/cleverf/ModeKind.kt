/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

import com.maddyhome.idea.vim.state.mode.Mode

/** The modes clever-f keeps a separate search for */
internal enum class ModeKind {
  NORMAL,
  VISUAL,
  OP_PENDING;

  companion object {
    fun of(mode: Mode): ModeKind = when (mode) {
      is Mode.OP_PENDING -> OP_PENDING
      is Mode.VISUAL, is Mode.SELECT -> VISUAL
      else -> NORMAL
    }
  }
}
