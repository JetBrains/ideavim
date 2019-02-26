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
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.command.VisualChange
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.CaretData
import com.maddyhome.idea.vim.helper.EditorData
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.option.BoundStringOption
import com.maddyhome.idea.vim.option.Options
import java.util.*

/**
 * @author Alex Plate
 */
object VisualMotionGroup {
    fun selectPreviousVisualMode(editor: Editor): Boolean {
        val lastSelectionType = EditorData.getLastSelectionType(editor) ?: return false

        val visualMarks = VimPlugin.getMark().getVisualSelectionMarks(editor) ?: return false

        editor.caretModel.removeSecondaryCarets()

        CommandState.getInstance(editor)
                .pushState(CommandState.Mode.VISUAL, lastSelectionType.toSubMode(), MappingMode.VISUAL)

        val primaryCaret = editor.caretModel.primaryCaret

        CaretData.setVisualStart(primaryCaret, visualMarks.startOffset)
        CaretData.setVisualEnd(primaryCaret, visualMarks.endOffset)
        CaretData.setVisualOffset(primaryCaret, visualMarks.endOffset)

        updateSelection(editor, primaryCaret, visualMarks.endOffset)

        primaryCaret.moveToOffset(visualMarks.endOffset)
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)

        return true
    }

    fun swapVisualSelections(editor: Editor): Boolean {
        val lastSelectionType = EditorData.getLastSelectionType(editor) ?: return false
        val lastVisualRange = EditorData.getLastVisualRange(editor) ?: return false

        val selectionType = SelectionType.fromSubMode(CommandState.getInstance(editor).subMode)
        EditorData.setLastSelectionType(editor, selectionType)

        editor.caretModel.removeSecondaryCarets()

        val primaryCaret = editor.caretModel.primaryCaret
        CaretData.setVisualStart(primaryCaret, lastVisualRange.startOffset)
        CaretData.setVisualEnd(primaryCaret, lastVisualRange.endOffset)
        CaretData.setVisualOffset(primaryCaret, lastVisualRange.endOffset)

        CommandState.getInstance(editor).subMode = lastSelectionType.toSubMode()

        updateSelection(editor, primaryCaret, lastVisualRange.endOffset)

        primaryCaret.moveToOffset(lastVisualRange.endOffset)
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)

        return true
    }

    fun setVisualMode(editor: Editor, mode: CommandState.SubMode) {
        var autodetectedMode = mode
        val oldMode = CommandState.getInstance(editor).subMode
        if (autodetectedMode == CommandState.SubMode.NONE) {
            // Detect if visual mode is character wise or line wise
            val start = editor.selectionModel.selectionStart
            val end = editor.selectionModel.selectionEnd
            if (start != end) {
                val line = editor.offsetToLogicalPosition(start).line
                val logicalStart = EditorHelper.getLineStartOffset(editor, line)
                val lend = EditorHelper.getLineEndOffset(editor, line, true)
                autodetectedMode = if (logicalStart == start && lend + 1 == end) {
                    CommandState.SubMode.VISUAL_LINE
                } else {
                    CommandState.SubMode.VISUAL_CHARACTER
                }
            }
        }

        if (oldMode == CommandState.SubMode.NONE && autodetectedMode == CommandState.SubMode.NONE) {
            // Visual mode was not enabled and new mode is not visual. Delete selection of caret
            editor.selectionModel.removeSelection()
            return
        }

        if (autodetectedMode == CommandState.SubMode.NONE) {
            VisualMotionGroup.exitVisual(editor)
        } else {
            CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, autodetectedMode, MappingMode.VISUAL)
        }

        KeyHandler.getInstance().reset(editor)

        for (caret in editor.caretModel.allCarets) {
            CaretData.setVisualStart(caret, caret.selectionStart)
            var visualEnd = caret.selectionEnd
            if (CommandState.getInstance(editor).subMode == CommandState.SubMode.VISUAL_CHARACTER) {
                val opt = Options.getInstance().getOption("selection") as BoundStringOption
                visualEnd -= if (opt.value == "exclusive") 0 else 1
            }
            CaretData.setVisualEnd(caret, visualEnd)
            CaretData.setVisualOffset(caret, caret.offset)
        }

        VimPlugin.getMark().setVisualSelectionMarks(editor, getRawVisualRange(editor.caretModel.primaryCaret))
    }

    /**
     * This function toggles visual mode.
     *
     * If visual mode is disabled, enable it
     * If visual mode is enabled, but [mode] differs, update visual according to new [mode]
     * If visual mode is enabled with the same [mode], disable it
     */
    fun toggleVisual(editor: Editor, count: Int, rawCount: Int, mode: CommandState.SubMode): Boolean {
        if (CommandState.getInstance(editor).mode != CommandState.Mode.VISUAL) {
            // Enable visual mode
            if (rawCount > 0) {
                if (editor.caretModel.caretCount > 1) {
                    return false
                }
                val range = CaretData.getLastVisualOperatorRange(editor.caretModel.primaryCaret) ?: return false
                val newMode = range.type.toSubMode()
                val start = editor.caretModel.offset
                val end = calculateVisualRange(editor, range, count)
                val primaryCaret = editor.caretModel.primaryCaret
                CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, newMode, MappingMode.VISUAL)
                CaretData.setVisualStart(primaryCaret, start)
                updateSelection(editor, primaryCaret, end)
                MotionGroup.moveCaret(editor, primaryCaret, CaretData.getVisualEnd(primaryCaret), true)
            } else {
                CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, mode, MappingMode.VISUAL)
                if (mode == CommandState.SubMode.VISUAL_BLOCK) {
                    EditorData.setVisualBlockStart(editor, editor.selectionModel.selectionStart)
                    updateBlockSelection(editor, editor.selectionModel.selectionEnd)
                    MotionGroup
                            .moveCaret(editor, editor.caretModel.primaryCaret, EditorData.getVisualBlockEnd(editor), true)
                } else {
                    for (caret in editor.caretModel.allCarets) {
                        CaretData.setVisualStart(caret, caret.selectionStart)
                        updateSelection(editor, caret, caret.selectionEnd)
                        MotionGroup.moveCaret(editor, caret, CaretData.getVisualEnd(caret), true)
                    }
                }
            }
            return true
        }

        if (mode == CommandState.getInstance(editor).subMode) {
            // Disable visual mode
            VisualMotionGroup.exitVisual(editor)
            return true
        }

        // Update visual mode with new sub mode
        CommandState.getInstance(editor).subMode = mode
        if (mode == CommandState.SubMode.VISUAL_BLOCK) {
            updateBlockSelection(editor, EditorData.getVisualBlockEnd(editor))
        } else {
            for (caret in editor.caretModel.allCarets) {
                updateSelection(editor, caret, CaretData.getVisualEnd(caret))
            }
        }

        return true
    }

    private fun calculateVisualRange(editor: Editor, range: VisualChange, count: Int): Int {
        var lines = range.lines
        var chars = range.columns
        if (range.type == SelectionType.LINE_WISE || range.type == SelectionType.BLOCK_WISE || lines > 1) {
            lines *= count
        }
        if (range.type == SelectionType.CHARACTER_WISE && lines == 1 || range.type == SelectionType.BLOCK_WISE) {
            chars *= count
        }
        val start = editor.caretModel.offset
        val sp = editor.offsetToLogicalPosition(start)
        val endLine = sp.line + lines - 1

        return if (range.type == SelectionType.LINE_WISE) {
            VimPlugin.getMotion().moveCaretToLine(editor, endLine)
        } else if (range.type == SelectionType.CHARACTER_WISE) {
            if (lines > 1) {
                VimPlugin.getMotion().moveCaretToLineStart(editor, endLine) + Math.min(EditorHelper.getLineLength(editor, endLine), chars)
            } else {
                EditorHelper.normalizeOffset(editor, sp.line, start + chars - 1, false)
            }
        } else {
            val endColumn = Math.min(EditorHelper.getLineLength(editor, endLine), sp.column + chars - 1)
            editor.logicalPositionToOffset(LogicalPosition(endLine, endColumn))
        }
    }

    fun getVisualOperatorRange(editor: Editor, caret: Caret, cmdFlags: EnumSet<CommandFlags>): VisualChange {
        var start = CaretData.getVisualStart(caret)
        var end = CaretData.getVisualEnd(caret)

        if (CommandState.inVisualBlockMode(editor)) {
            start = EditorData.getVisualBlockStart(editor)
            end = EditorData.getVisualBlockEnd(editor)
        }

        if (start > end) {
            val t = start
            start = end
            end = t
        }

        start = EditorHelper.normalizeOffset(editor, start, false)
        end = EditorHelper.normalizeOffset(editor, end, false)
        val sp = editor.offsetToLogicalPosition(start)
        val ep = editor.offsetToLogicalPosition(end)
        val lines = ep.line - sp.line + 1
        val (type, chars) = if (CommandState.getInstance(editor).subMode == CommandState.SubMode.VISUAL_LINE || CommandFlags.FLAG_MOT_LINEWISE in cmdFlags) {
            SelectionType.LINE_WISE to ep.column
        } else if (CommandState.getInstance(editor).subMode == CommandState.SubMode.VISUAL_CHARACTER) {
            SelectionType.CHARACTER_WISE to if (lines > 1) {
                ep.column
            } else {
                ep.column - sp.column + 1
            }
        } else {
            SelectionType.BLOCK_WISE to if (CaretData.getLastColumn(editor.caretModel.primaryCaret) == MotionGroup.LAST_COLUMN) {
                MotionGroup.LAST_COLUMN
            } else ep.column - sp.column + 1
        }

        return VisualChange(lines, chars, type)
    }

    fun getVisualRange(editor: Editor) = editor.selectionModel.run { TextRange(blockSelectionStarts, blockSelectionEnds) }

    fun getVisualRange(caret: Caret) = TextRange(caret.selectionStart, caret.selectionEnd)

    fun getRawVisualRange(caret: Caret) = TextRange(CaretData.getVisualStart(caret), CaretData.getVisualEnd(caret))

    fun updateBlockSelection(editor: Editor) = updateBlockSelection(editor, EditorData.getVisualBlockEnd(editor))

    private fun updateBlockSelection(editor: Editor, offset: Int) {
        EditorData.setVisualBlockEnd(editor, offset)
        EditorData.setVisualBlockOffset(editor, offset)
        val start = EditorData.getVisualBlockStart(editor)
        val end = EditorData.getVisualBlockEnd(editor)

        var blockStart = editor.offsetToLogicalPosition(start)
        var blockEnd = editor.offsetToLogicalPosition(end)
        if (blockStart.column < blockEnd.column) {
            blockEnd = LogicalPosition(blockEnd.line, blockEnd.column + 1)
        } else {
            blockStart = LogicalPosition(blockStart.line, blockStart.column + 1)
        }
        editor.selectionModel.setBlockSelection(blockStart, blockEnd)

        for (caret in editor.caretModel.allCarets) {
            val line = caret.logicalPosition.line
            val lineEndOffset = EditorHelper.getLineEndOffset(editor, line, true)

            if (CaretData.getLastColumn(editor.caretModel.primaryCaret) >= MotionGroup.LAST_COLUMN) {
                caret.setSelection(caret.selectionStart, lineEndOffset)
            }
            if (!EditorHelper.isLineEmpty(editor, line, false)) {
                caret.moveToOffset(caret.selectionEnd - 1)
            }
        }
        editor.caretModel.primaryCaret.moveToOffset(end)

        VimPlugin.getMark().setVisualSelectionMarks(editor, TextRange(start, end))
    }

    fun updateSelection(editor: Editor, caret: Caret, offset: Int) {
        if (CommandState.getInstance(editor).subMode == CommandState.SubMode.VISUAL_BLOCK) {
            VisualMotionGroup.updateBlockSelection(editor, offset)
            return
        }

        CaretData.setVisualEnd(caret, offset)
        CaretData.setVisualOffset(caret, offset)
        var start = CaretData.getVisualStart(caret)
        var end = offset
        val subMode = CommandState.getInstance(editor).subMode

        if (start > end) {
            val t = start
            start = end
            end = t
        }
        if (subMode == CommandState.SubMode.VISUAL_CHARACTER) {
            val opt = Options.getInstance().getOption("selection") as BoundStringOption
            val lineEnd = EditorHelper.getLineEndForOffset(editor, end)
            val adj = if (opt.value == "exclusive" || end == lineEnd) 0 else 1
            val adjEnd = minOf(EditorHelper.getFileSize(editor), end + adj)
            caret.setSelection(start, adjEnd)
        } else if (subMode == CommandState.SubMode.VISUAL_LINE) {
            start = EditorHelper.getLineStartForOffset(editor, start)
            end = EditorHelper.getLineEndForOffset(editor, end)
            caret.setSelection(start, end)
        }

        VimPlugin.getMark().setVisualSelectionMarks(editor, TextRange(start, end))
    }

    fun swapVisualBlockEnds(editor: Editor): Boolean {
        if (!CommandState.inVisualBlockMode(editor)) return false
        val t = EditorData.getVisualBlockEnd(editor)
        EditorData.setVisualBlockEnd(editor, EditorData.getVisualBlockStart(editor))
        EditorData.setVisualBlockStart(editor, t)

        MotionGroup.moveCaret(editor, editor.caretModel.primaryCaret, EditorData.getVisualBlockEnd(editor))

        return true
    }

    fun swapVisualEnds(editor: Editor, caret: Caret): Boolean {
        val t = CaretData.getVisualEnd(caret)
        CaretData.setVisualEnd(caret, CaretData.getVisualStart(caret))
        CaretData.setVisualStart(caret, t)

        MotionGroup.moveCaret(editor, caret, CaretData.getVisualEnd(caret))

        return true
    }

    fun processEscape(editor: Editor) = VisualMotionGroup.exitVisual(editor)

    fun resetVisual(editor: Editor, removeSelection: Boolean) {
        val wasVisualBlock = CommandState.inVisualBlockMode(editor)
        val selectionType = SelectionType.fromSubMode(CommandState.getInstance(editor).subMode)
        EditorData.setLastSelectionType(editor, selectionType)
        VimPlugin.getMark().getVisualSelectionMarks(editor)?.let {
            EditorData.setLastVisualRange(editor, it)
        }
        if (removeSelection) {
            if (!EditorData.isKeepingVisualOperatorAction(editor)) {
                for (caret in editor.caretModel.allCarets) {
                    caret.removeSelection()
                }
            }
            if (wasVisualBlock) {
                editor.caretModel.removeSecondaryCarets()
            }
        }
        CommandState.getInstance(editor).subMode = CommandState.SubMode.NONE
    }

    fun exitVisual(editor: Editor) {
        resetVisual(editor, true)
        if (CommandState.getInstance(editor).mode == CommandState.Mode.VISUAL) {
            CommandState.getInstance(editor).popState()
        }
    }


    fun moveVisualStart(caret: Caret, startOffset: Int) = CaretData.setVisualStart(caret, startOffset)
}