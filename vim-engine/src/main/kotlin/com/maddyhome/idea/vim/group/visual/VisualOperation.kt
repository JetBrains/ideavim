/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.api.normalizeOffset
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.subMode
import java.util.*
import kotlin.math.min

public object VisualOperation {
  /**
   * Get [VisualChange] of current visual operation
   */
  public fun getRange(editor: VimEditor, caret: ImmutableVimCaret, cmdFlags: EnumSet<CommandFlags>): VisualChange {
    var (start, end) = caret.run {
      if (editor.inBlockSubMode) sort(vimSelectionStart, offset.point) else sort(selectionStart, selectionEnd)
    }
    val type = SelectionType.fromSubMode(editor.subMode)

    start = editor.normalizeOffset(start, false)
    end = editor.normalizeOffset(end, false)
    val sp = editor.offsetToBufferPosition(start)
    val ep = editor.offsetToBufferPosition(end)
    var lines = ep.line - sp.line + 1
    if (type == SelectionType.LINE_WISE && ep.column == 0 && lines > 0) lines--

    if (CommandFlags.FLAG_MOT_LINEWISE in cmdFlags) return VisualChange(lines, ep.column, SelectionType.LINE_WISE)

    val chars = if (editor.primaryCaret().vimLastColumn == VimMotionGroupBase.LAST_COLUMN) {
      VimMotionGroupBase.LAST_COLUMN
    } else {
      when (type) {
        SelectionType.LINE_WISE -> ep.column
        SelectionType.CHARACTER_WISE -> if (lines > 1) ep.column - injector.visualMotionGroup.selectionAdj else ep.column - sp.column
        SelectionType.BLOCK_WISE -> ep.column - sp.column + 1
      }
    }

    return VisualChange(lines, chars, type)
  }

  /**
   * Calculate end offset of [VisualChange]
   */
  public fun calculateRange(editor: VimEditor, range: VisualChange, count: Int, caret: ImmutableVimCaret): Int {
    var (lines, chars, type) = range
    if (type == SelectionType.LINE_WISE || type == SelectionType.BLOCK_WISE || lines > 1) {
      lines *= count
    }
    if (type == SelectionType.CHARACTER_WISE && lines == 1 || type == SelectionType.BLOCK_WISE) {
      chars *= count
    }
    val sp = caret.getBufferPosition()
    val linesDiff = (lines - 1).coerceAtLeast(0)
    val endLine = (sp.line + linesDiff).coerceAtMost(editor.lineCount() - 1)

    return when (type) {
      SelectionType.LINE_WISE -> injector.motion.moveCaretToLineWithSameColumn(editor, endLine, caret)
      SelectionType.CHARACTER_WISE -> when {
        lines > 1 -> injector.motion.moveCaretToLineStart(editor, endLine) + min(editor.lineLength(endLine), chars)
        else -> editor.normalizeOffset(sp.line, caret.offset.point + chars - 1, true)
      }
      SelectionType.BLOCK_WISE -> {
        val endColumn = min(editor.lineLength(endLine), sp.column + chars - 1)
        editor.bufferPositionToOffset(BufferPosition(endLine, endColumn))
      }
    }
  }
}
