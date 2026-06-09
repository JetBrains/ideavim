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
 * Identifies which kind of virtual buffer the editor is hosting: a command-line / search history
 * window (`q:`, `q/`, `q?`) or the control-chars editor.
 */
sealed class VirtualBufferKind {
  abstract val fileName: String

  data object Command : VirtualBufferKind() {
    override val fileName: String = "[Command Line]"
  }

  data class Search(val direction: Direction) : VirtualBufferKind() {
    override val fileName: String = "[Search Line]"
  }

  data object ControlCharsEditor : VirtualBufferKind() {
    override val fileName: String = "[Control Chars Editor]"
  }
}
