/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.mark

/**
 * Where a [Jump] was recorded, which decides where it brings the user back to
 */
sealed interface JumpSource {
  /** A plain editor showing the file */
  data object File : JumpSource

  /**
   * One side of a diff shown in an editor tab
   *
   * @param diffId tells the diff tabs apart while they are open. It means nothing after a restart.
   * @param isInFile whether the side shows the file itself, rather than e.g. an older revision of it. Only such a side
   *   can still be found once its diff is closed.
   */
  data class Diff(val diffId: String, val side: DiffSide, val isInFile: Boolean) : JumpSource
}

enum class DiffSide { LEFT, RIGHT }
