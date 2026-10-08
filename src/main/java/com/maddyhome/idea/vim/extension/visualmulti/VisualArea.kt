/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.selectionType
import kotlin.math.max
import kotlin.math.min

/**
 * The lines of the Visual selection, and the column where it starts, like the `'<` mark
 *
 * The `'<` mark is in the first column for a linewise selection, and in the left column for a blockwise one.
 */
internal class VisualArea(val startLine: Int, val endLine: Int, val startColumn: Int, val type: SelectionType) {

  val lines: IntRange
    get() = startLine..endLine

  companion object {
    fun of(editor: VimEditor): VisualArea? {
      val type = editor.mode.selectionType ?: return null
      val caret = editor.primaryCaret()
      val anchor = editor.offsetToBufferPosition(caret.vimSelectionStart)
      val head = editor.offsetToBufferPosition(caret.offset)
      val start = minOf(anchor, head)

      val startColumn = when (type) {
        SelectionType.CHARACTER_WISE -> start.column
        SelectionType.LINE_WISE -> 0
        SelectionType.BLOCK_WISE -> min(anchor.column, head.column)
      }
      return VisualArea(min(anchor.line, head.line), max(anchor.line, head.line), startColumn, type)
    }
  }
}
