/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.sort
import com.maddyhome.idea.vim.helper.subMode
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.helper.vimSelectionStart
import java.util.*
import kotlin.math.min

data class VisualChange(val lines: Int, val columns: Int, val type: SelectionType) {
  companion object {
    fun default(subMode: CommandState.SubMode) =
      when (val type = SelectionType.fromSubMode(subMode)) {
        SelectionType.LINE_WISE, SelectionType.CHARACTER_WISE -> VisualChange(1, 1, type)
        SelectionType.BLOCK_WISE -> VisualChange(0, 1, type)
      }
  }
}

object VisualOperation {
  /**
   * Get [VisualChange] of current visual operation
   */
  fun getRange(editor: Editor, caret: Caret, cmdFlags: EnumSet<CommandFlags>): VisualChange {
    var (start, end) = caret.run {
      if (editor.inBlockSubMode) sort(vimSelectionStart, offset) else sort(selectionStart, selectionEnd)
    }
    val type = SelectionType.fromSubMode(editor.subMode)

    start = EditorHelper.normalizeOffset(editor, start, false)
    end = EditorHelper.normalizeOffset(editor, end, false)
    val sp = editor.offsetToLogicalPosition(start)
    val ep = editor.offsetToLogicalPosition(end)
    var lines = ep.line - sp.line + 1
    if (type == SelectionType.LINE_WISE && ep.column == 0 && lines > 0) lines--

    if (CommandFlags.FLAG_MOT_LINEWISE in cmdFlags) return VisualChange(lines, ep.column, SelectionType.LINE_WISE)

    val chars = if (editor.caretModel.primaryCaret.vimLastColumn == MotionGroup.LAST_COLUMN) {
      MotionGroup.LAST_COLUMN
    } else when (type) {
      SelectionType.LINE_WISE -> ep.column
      SelectionType.CHARACTER_WISE -> if (lines > 1) ep.column - VimPlugin.getVisualMotion().selectionAdj else ep.column - sp.column
      SelectionType.BLOCK_WISE -> ep.column - sp.column + 1
    }

    return VisualChange(lines, chars, type)
  }

  /**
   * Calculate end offset of [VisualChange]
   */
  fun calculateRange(editor: Editor, range: VisualChange, count: Int, caret: Caret): Int {
    var (lines, chars, type) = range
    if (type == SelectionType.LINE_WISE || type == SelectionType.BLOCK_WISE || lines > 1) {
      lines *= count
    }
    if (type == SelectionType.CHARACTER_WISE && lines == 1 || type == SelectionType.BLOCK_WISE) {
      chars *= count
    }
    val sp = caret.logicalPosition
    val linesDiff = (lines - 1).coerceAtLeast(0)
    val endLine = (sp.line + linesDiff).coerceAtMost(editor.document.lineCount - 1)

    return when (type) {
      SelectionType.LINE_WISE -> VimPlugin.getMotion().moveCaretToLine(editor, endLine, caret)
      SelectionType.CHARACTER_WISE -> when {
        lines > 1 -> VimPlugin.getMotion()
          .moveCaretToLineStart(editor, endLine) + min(EditorHelper.getLineLength(editor, endLine), chars)
        else -> EditorHelper.normalizeOffset(editor, sp.line, caret.offset + chars - 1, true)
      }
      SelectionType.BLOCK_WISE -> {
        val endColumn = min(EditorHelper.getLineLength(editor, endLine), sp.column + chars - 1)
        editor.logicalPositionToOffset(LogicalPosition(endLine, endColumn))
      }
    }
  }
}
