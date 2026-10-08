/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.api.normalizeColumn
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.multiplecursors.leaveVisualMode
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * Leaves Visual mode with a cursor on each line of the selection, in the column where it starts
 *
 * The lines below the first one are skipped like with Add Cursor Down. For a linewise selection, the cursors are in the
 * first column, and the empty lines are skipped.
 */
internal class VisualCursorsHandler : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val area = VisualArea.of(editor) ?: return
    val skipping = if (area.type == SelectionType.LINE_WISE) {
      LineSkipping(shorterLines = true, emptyLines = true)
    } else {
      LineSkipping.read()
    }

    leaveVisualMode(editor)
    editor.removeSecondaryCarets()
    editor.primaryCaret().moveToOffset(offsetOf(editor, area.startLine, area.startColumn))
    for (line in area.lines.drop(1)) {
      if (!skipping.skips(editor.lineLength(line), area.startColumn)) {
        editor.addCaret(offsetOf(editor, line, area.startColumn))
      }
    }
  }

  private fun offsetOf(editor: VimEditor, line: Int, column: Int): Int =
    editor.bufferPositionToOffset(BufferPosition(line, editor.normalizeColumn(line, column, false)))
}
