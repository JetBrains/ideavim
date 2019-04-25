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
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.SelectionType.*
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.toNativeSelection
import kotlin.math.max
import kotlin.math.min

/**
 * @author Alex Plate
 *
 * Class for storing selection range.
 *   [start] and [end] are the offsets of native selection and type of selection is stored in [type]
 *
 * [vimStart] and [vimEnd] - selection offsets in vim model. There values will be stored in '< and '> marks.
 *   There values can differ from [start] and [end] in case of linewise selection because [vimStart] - initial caret
 *   position when visual mode entered and [vimEnd] - current caret position.
 *
 * This selection has direction. That means that by moving in left-up direction (e.g. `vbbbb`)
 *   [vimStart] and [start] will be greater then [vimEnd] and [end].
 * If you need normalized [start] and [end] (start always less than end) you
 *   can use [normStart] and [normEnd] properties.this.normStart = min(start, end)
 *
 * All starts are included and ends are excluded
 */
class VimSelection {
    /**
     * Native selection start. Inclusive. Directional.
     */
    val start: Int
    /**
     * Native selection end. Exclusive. Directional.
     */
    val end: Int

    /**
     * Vim selection start.
     * This value can differ from [start] in case of linewise selection because
     *   [vimStart] - initial caret position when visual mode entered
     */
    val vimStart: Int
    /**
     * Vim selection end.
     * This value can differ from [end] in case of linewise selection because [vimEnd] - current caret position.
     */
    val vimEnd: Int

    /**
     * Native selection start. Inclusive. Non-directional.
     */
    val normStart: Int
    /**
     * Native selection end. Exclusive. Non-directional.
     */
    val normEnd: Int

    val type: SelectionType
    val editor: Editor

    constructor(
            nativeStart: Int,
            nativeEnd: Int,
            vimStart: Int,
            vimEnd: Int,
            type: SelectionType,
            editor: Editor
    ) {
        this.vimStart = vimStart
        this.vimEnd = vimEnd
        this.type = type
        this.editor = editor
        this.start = nativeStart
        this.end = nativeEnd

        this.normStart = min(start, end)
        this.normEnd = max(start, end)
    }

    /**
     * [start] and [end] are calculated based on [vimStart] and [vimEnd] properties
     */
    constructor(
            vimStart: Int,
            vimEnd: Int,
            type: SelectionType,
            editor: Editor
    ) {
        this.vimStart = vimStart
        this.vimEnd = vimEnd
        this.type = type
        this.editor = editor

        val nativeStartAndEnd = toNativeSelection(editor, vimStart, vimEnd, CommandState.Mode.VISUAL, type.toSubMode())
        this.start = nativeStartAndEnd.first
        this.end = nativeStartAndEnd.second
        this.normStart = min(start, end)
        this.normEnd = max(start, end)
    }

    /**
     * Converting to an old TextRange class
     */
    fun toVimTextRange(skipNewLineForLineMode: Boolean) = when (type) {
        CHARACTER_WISE -> TextRange(normStart, normEnd)
        LINE_WISE -> {
            if (skipNewLineForLineMode && editor.document.text[normEnd - 1] == '\n') {
                TextRange(normStart, (normEnd - 1).coerceAtLeast(0))
            } else {
                TextRange(normStart, normEnd)
            }
        }
        BLOCK_WISE -> {
            val logicalStart = editor.offsetToLogicalPosition(normStart)
            val logicalEnd = editor.offsetToLogicalPosition(normEnd)
            val lineRange = if (logicalStart.line > logicalEnd.line) logicalStart.line downTo logicalEnd.line else logicalStart.line..logicalEnd.line
            val starts = ArrayList<Int>()
            val ends = ArrayList<Int>()
            for (line in lineRange) {
                starts += editor.logicalPositionToOffset(LogicalPosition(line, logicalStart.column))
                ends += editor.logicalPositionToOffset(LogicalPosition(line, logicalEnd.column))
            }
            TextRange(starts.toIntArray(), ends.toIntArray()).also { it.normalize(editor.document.textLength) }
        }
    }

    /**
     * Execute [action] for each line of selection.
     * Action will be executed in bottom-up direction if [start] > [end]
     *
     * [action#startOffset] and [action#endOffset] are offsets in current line
     */
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
