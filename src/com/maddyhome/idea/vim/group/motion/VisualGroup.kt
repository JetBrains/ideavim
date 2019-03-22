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

package com.maddyhome.idea.vim.group.motion

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretVisualAttributes
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.helper.vimSelectionStart

/**
 * @author Alex Plate
 */
val Editor.visualBlockRange: TextRange
    get() = selectionModel.run { TextRange(blockSelectionStarts, blockSelectionEnds) }

fun Caret.vimStartSelectionAtPoint(point: Int) {
    vimSelectionStart = point
    setVisualSelection(point, point, this)
}

fun Caret.vimMoveSelectionToCaret() {
    if (CommandState.getInstance(editor).mode != CommandState.Mode.VISUAL)
        throw RuntimeException("Attempt to extent selection in non-visual mode")
    if (CommandState.inVisualBlockMode(editor))
        throw RuntimeException("Move caret with [vimMoveBlockSelectionToOffset]")

    val startOffsetMark = vimSelectionStart

    setVisualSelection(startOffsetMark, offset, this)
}

fun vimMoveBlockSelectionToOffset(editor: Editor, offset: Int) {
    if (!CommandState.inVisualBlockMode(editor))
        throw RuntimeException("Move caret with [vimMoveSelectionToCaret]")

    val primaryCaret = editor.caretModel.primaryCaret
    val startOffsetMark = primaryCaret.vimSelectionStart

    setVisualSelection(startOffsetMark, offset, primaryCaret)
}

fun Caret.vimUpdateEditorSelection() {
    val startOffsetMark = vimSelectionStart
    setVisualSelection(startOffsetMark, offset, this)
}

var vimSuppressSelectionListener = false

private fun setVisualSelection(selectionStart: Int, selectionEnd: Int, caret: Caret) {
    val (start, end) = if (selectionStart > selectionEnd) selectionEnd to selectionStart else selectionStart to selectionEnd
    val editor = caret.editor
    val subMode = CommandState.getInstance(editor).subMode
    vimSuppressSelectionListener = true
    when (subMode) {
        CommandState.SubMode.VISUAL_LINE -> {
            val lineStart = EditorHelper.getLineStartForOffset(editor, start)
            val lineEnd = EditorHelper.getLineEndForOffset(editor, end)
            caret.setSelection(lineStart, lineEnd)
        }
        CommandState.SubMode.VISUAL_CHARACTER -> {
            val lineEnd = EditorHelper.getLineEndForOffset(editor, end)
            val adj = if (VisualMotionGroup.exclusiveSelection || end == lineEnd) 0 else 1
            val adjEnd = (end + adj).coerceAtMost(EditorHelper.getFileSize(editor))
            caret.setSelection(start, adjEnd)
        }
        CommandState.SubMode.VISUAL_BLOCK -> {
            editor.caretModel.removeSecondaryCarets()

            var blockStart = editor.offsetToLogicalPosition(selectionStart)
            var blockEnd = editor.offsetToLogicalPosition(selectionEnd)
            if (!VisualMotionGroup.exclusiveSelection) {
                if (blockStart.column > blockEnd.column) {
                    blockStart = LogicalPosition(blockStart.line, blockStart.column + 1)
                } else {
                    blockEnd = LogicalPosition(blockEnd.line, blockEnd.column + 1)
                }
            }
            val lastColumn = editor.caretModel.primaryCaret.vimLastColumn
            editor.selectionModel.setBlockSelection(blockStart, blockEnd)

            for (aCaret in editor.caretModel.allCarets) {
                val line = aCaret.logicalPosition.line
                val lineEndOffset = EditorHelper.getLineEndOffset(editor, line, true)

                if (lastColumn >= MotionGroup.LAST_COLUMN) {
                    aCaret.setSelection(aCaret.selectionStart, lineEndOffset)
                }
                if (!EditorHelper.isLineEmpty(editor, line, false)) {
                    aCaret.moveToOffset(aCaret.selectionEnd - 1)
                }

                if (aCaret != editor.caretModel.primaryCaret) {
                    val color = editor.colorsScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR)
                    val visualAttributes = aCaret.visualAttributes
                    aCaret.visualAttributes = CaretVisualAttributes(color, visualAttributes.weight)
                }
            }

            editor.caretModel.primaryCaret.moveToOffset(selectionEnd)
        }
    }
    vimSuppressSelectionListener = false
}