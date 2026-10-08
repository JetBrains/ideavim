/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.maddyhome.idea.vim.extension.multiplecursors.isGlobalFlagEnabled

/**
 * `g:VM_skip_shorter_lines` and `g:VM_skip_empty_lines`
 */
internal class LineSkipping(private val shorterLines: Boolean, private val emptyLines: Boolean) {

  /**
   * A line is shorter when it doesn't reach the column. An empty line reaches the first column, unless empty lines are
   * skipped. vim-visual-multi checks empty lines only when it skips shorter lines.
   */
  fun skips(lineLength: Int, column: Int): Boolean = when {
    !shorterLines -> false
    lineLength == 0 -> emptyLines || column > 0
    else -> lineLength <= column
  }

  companion object {
    fun read() = LineSkipping(
      shorterLines = isGlobalFlagEnabled("VM_skip_shorter_lines"),
      emptyLines = isGlobalFlagEnabled("VM_skip_empty_lines", default = false),
    )
  }
}
