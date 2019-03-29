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

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.colors.EditorColors
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.helper.vimSelectionStart

/**
 * @author Alex Plate
 */

/**
 * Get deprecated TextRange of editor's selection model
 */
val Editor.visualBlockRange: TextRange
    get() = selectionModel.run { TextRange(blockSelectionStarts, blockSelectionEnds) }

/**
 * Start selection of caret at given point.
 * This method doesn't change CommandState and operates only with caret and it's properties
 */
fun Caret.vimStartSelectionAtPoint(point: Int) {
    vimSelectionStart = point
    setVisualSelection(point, point, this)
}

/**
 * Move selection end to current caret position
 * This method is created only for Character and Line mode
 * @see vimMoveBlockSelectionToOffset for blockwise selection
 */
fun Caret.vimMoveSelectionToCaret() {
    if (CommandState.getInstance(editor).mode != CommandState.Mode.VISUAL && CommandState.getInstance(editor).mode != CommandState.Mode.SELECT)
        throw RuntimeException("Attempt to extent selection in non-visual mode")
    if (CommandState.inVisualBlockMode(editor))
        throw RuntimeException("Move caret with [vimMoveBlockSelectionToOffset]")

    val startOffsetMark = vimSelectionStart

    setVisualSelection(startOffsetMark, offset, this)
}

/**
 * Move selection end to current primary caret position
 * This method is created only for block mode
 * @see vimMoveSelectionToCaret for character and line selection
 */
fun vimMoveBlockSelectionToOffset(editor: Editor, offset: Int) {
    val primaryCaret = editor.caretModel.primaryCaret
    val startOffsetMark = primaryCaret.vimSelectionStart

    setVisualSelection(startOffsetMark, offset, primaryCaret)
}

/**
 * Update selection according to new CommandState
 * This method should be used for switching from character to line wise selection and so on
 */
fun Caret.vimUpdateEditorSelection() {
    val startOffsetMark = vimSelectionStart
    setVisualSelection(startOffsetMark, offset, this)
}

/**
 * Set selection without calling SelectionListener
 */
fun SelectionModel.vimSetSelectionSilently(start: Int, end: Int) {
    SelectionVimListenerSuppressor.lock()
    setSelection(start, end)
    SelectionVimListenerSuppressor.unlock()
}

/**
 * Set selection without calling SelectionListener
 */
fun SelectionModel.vimSetBlockSelectionSilently(start: LogicalPosition, end: LogicalPosition) {
    SelectionVimListenerSuppressor.lock()
    setBlockSelection(start, end)
    SelectionVimListenerSuppressor.unlock()
}

/**
 * Set selection without calling SelectionListener
 */
fun Caret.vimSetSelectionSilently(start: Int, end: Int) {
    SelectionVimListenerSuppressor.lock()
    setSelection(start, end)
    SelectionVimListenerSuppressor.unlock()
}

private fun setVisualSelection(selectionStart: Int, selectionEnd: Int, caret: Caret) {
    val (start, end) = if (selectionStart > selectionEnd) selectionEnd to selectionStart else selectionStart to selectionEnd
    val editor = caret.editor
    val subMode = CommandState.getInstance(editor).subMode
    val mode = CommandState.getInstance(editor).mode
    when (subMode) {
        CommandState.SubMode.VISUAL_LINE -> {
            val lineStart = EditorHelper.getLineStartForOffset(editor, start)
            val lineEnd = EditorHelper.getLineEndForOffset(editor, end)
            caret.vimSetSelectionSilently(lineStart, lineEnd)
        }
        CommandState.SubMode.VISUAL_CHARACTER -> {
            val lineEnd = EditorHelper.getLineEndForOffset(editor, end)
            val adj = if (VimPlugin.getVisualMotion().exclusiveSelection || end == lineEnd || mode == CommandState.Mode.SELECT) 0 else 1
            val adjEnd = (end + adj).coerceAtMost(EditorHelper.getFileSize(editor))
            caret.vimSetSelectionSilently(start, adjEnd)
        }
        CommandState.SubMode.VISUAL_BLOCK -> {
            editor.caretModel.removeSecondaryCarets()

            var blockStart = editor.offsetToLogicalPosition(selectionStart)
            var blockEnd = editor.offsetToLogicalPosition(selectionEnd)
            if (!VimPlugin.getVisualMotion().exclusiveSelection && mode != CommandState.Mode.SELECT) {
                if (blockStart.column > blockEnd.column) {
                    blockStart = LogicalPosition(blockStart.line, blockStart.column + 1)
                } else {
                    blockEnd = LogicalPosition(blockEnd.line, blockEnd.column + 1)
                }
            }
            val lastColumn = editor.caretModel.primaryCaret.vimLastColumn
            editor.selectionModel.vimSetBlockSelectionSilently(blockStart, blockEnd)

            for (aCaret in editor.caretModel.allCarets) {
                val line = aCaret.logicalPosition.line
                val lineEndOffset = EditorHelper.getLineEndOffset(editor, line, true)

                if (lastColumn >= MotionGroup.LAST_COLUMN) {
                    aCaret.vimSetSelectionSilently(aCaret.selectionStart, lineEndOffset)
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
}
