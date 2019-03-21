/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.SelectionType.BLOCK_WISE
import com.maddyhome.idea.vim.command.SelectionType.CHARACTER_WISE
import com.maddyhome.idea.vim.command.SelectionType.LINE_WISE
import com.maddyhome.idea.vim.common.TextRange

/**
 * @author Alex Plate
 */
data class VimSelection(
        val start: Int,
        val end: Int,
        val type: SelectionType,
        val editor: Editor
) {
    val normStart: Int = if (start > end) end else start
    val normEnd: Int = if (start > end) start else end

    fun toVimTextRange() = when (type) {
        CHARACTER_WISE, LINE_WISE -> TextRange(start, end)
        BLOCK_WISE -> {
            val logicalStart = editor.offsetToLogicalPosition(start)
            val logicalEnd = editor.offsetToLogicalPosition(end)
            val lineRange = if (logicalStart.line > logicalEnd.line) logicalStart.line downTo logicalEnd.line else logicalStart.line..logicalEnd.line
            val starts = ArrayList<Int>()
            val ends = ArrayList<Int>()
            for (line in lineRange) {
                starts += editor.logicalPositionToOffset(LogicalPosition(line, logicalStart.column))
                ends += editor.logicalPositionToOffset(LogicalPosition(line, logicalEnd.column))
            }
            TextRange(starts.toIntArray(), ends.toIntArray())
        }
    }

    inline fun forEachLine(action: (startOffset: Int, endOffset: Int) -> Unit) {
        val logicalStart = editor.offsetToLogicalPosition(start)
        val logicalEnd = editor.offsetToLogicalPosition(end)
        val lineRange = if (logicalStart.line > logicalEnd.line) logicalStart.line downTo logicalEnd.line else logicalStart.line..logicalEnd.line
        for (line in lineRange) {
            val start = editor.logicalPositionToOffset(LogicalPosition(line, logicalStart.column))
            val end = editor.logicalPositionToOffset(LogicalPosition(line, logicalEnd.column))
            action(start, end)
        }
    }

    override fun toString(): String {
        val startLogPosition = editor.offsetToLogicalPosition(start)
        val endLogPosition = editor.offsetToLogicalPosition(end)
        return "Selection [$type]: start[offset: $start : col ${startLogPosition.column} line ${startLogPosition.line}]" +
                " end[offset: $end : col ${endLogPosition.column} line ${endLogPosition.line}]"
    }
}
