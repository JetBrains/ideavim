/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.Direction

/**
 * Identifies which Vim command-line window the editor is hosting.
 *
 * - [Command]: `q:` window over `:` command history.
 * - [Search]: `q/` (forward) or `q?` (backward) window over `/` search history.
 */
sealed class HistoryWindowKind {
  abstract val fileName: String

  data object Command : HistoryWindowKind() {
    override val fileName: String = "[Command Line]"
  }

  data class Search(val direction: Direction) : HistoryWindowKind() {
    override val fileName: String = "[Search Line]"
  }
}
