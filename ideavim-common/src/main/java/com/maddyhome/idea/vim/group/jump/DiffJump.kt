/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.jump

import com.maddyhome.idea.vim.mark.DiffSide
import com.maddyhome.idea.vim.mark.Jump
import com.maddyhome.idea.vim.mark.JumpSource
import kotlinx.serialization.Serializable

/**
 * A [Jump] recorded in a diff tab, sent over [JumpRemoteApi]: in split mode, diff tabs live on the backend
 */
@Serializable
data class DiffJump(
  val line: Int,
  val col: Int,
  val filepath: String,
  val protocol: String,
  val diffId: String,
  val side: DiffSide,
  val isInFile: Boolean,
) {
  fun toJump(): Jump = Jump(line, col, filepath, protocol, JumpSource.Diff(diffId, side, isInFile))

  companion object {
    fun of(jump: Jump): DiffJump? {
      val source = jump.source as? JumpSource.Diff ?: return null
      return DiffJump(jump.line, jump.col, jump.filepath, jump.protocol, source.diffId, source.side, source.isInFile)
    }
  }
}
