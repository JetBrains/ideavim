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
import com.maddyhome.idea.vim.helper.EditorData
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.helper.vimLastVisualOperatorRange
import com.maddyhome.idea.vim.helper.vimSelectionStart
import com.maddyhome.idea.vim.helper.vimSelectionStartSetToNull
import com.maddyhome.idea.vim.helper.vimStartSelectionAtPoint
import com.maddyhome.idea.vim.helper.vimUpdateEditorSelection
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

        primaryCaret.vimStartSelectionAtPoint(visualMarks.startOffset)
        MotionGroup.moveCaret(editor, primaryCaret, visualMarks.endOffset)

        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)

        return true
    }

    fun swapVisualSelections(editor: Editor): Boolean {
        val lastSelectionType = EditorData.getLastSelectionType(editor) ?: return false

        val lastVisualRange = VimPlugin.getMark().getVisualSelectionMarks(editor) ?: return false
        val primaryCaret = editor.caretModel.primaryCaret
        editor.caretModel.removeSecondaryCarets()
        val vimSelectionStart = primaryCaret.vimSelectionStart

        val selectionType = SelectionType.fromSubMode(CommandState.getInstance(editor).subMode)
        EditorData.setLastSelectionType(editor, selectionType)
        VimPlugin.getMark().setVisualSelectionMarks(editor, TextRange(vimSelectionStart, primaryCaret.offset))

        CommandState.getInstance(editor).subMode = lastSelectionType.toSubMode()
        primaryCaret.vimStartSelectionAtPoint(lastVisualRange.startOffset)
        MotionGroup.moveCaret(editor, primaryCaret, lastVisualRange.endOffset)

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
            val line = editor.offsetToLogicalPosition(start).line
            val logicalStart = EditorHelper.getLineStartOffset(editor, line)
            val lend = EditorHelper.getLineEndOffset(editor, line, true)
            autodetectedMode = if (logicalStart == start && lend + 1 == end) {
                CommandState.SubMode.VISUAL_LINE
            } else {
                CommandState.SubMode.VISUAL_CHARACTER
            }
        }

        if (oldMode == CommandState.SubMode.NONE && autodetectedMode == CommandState.SubMode.NONE) {
            // Visual mode was not enabled and new mode is not visual. Delete selection of caret
            editor.selectionModel.removeSelection()
            return
        }

        if (autodetectedMode == CommandState.SubMode.NONE) {
            VisualMotionGroup.exitVisual(editor)
        } else if (autodetectedMode == oldMode) {
            return
        } else {
            CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, autodetectedMode, MappingMode.VISUAL)
            if (CommandState.inVisualBlockMode(editor)) {
                editor.caretModel.primaryCaret.let { it.vimStartSelectionAtPoint(it.offset) }
            } else {
                editor.caretModel.allCarets.forEach { it.vimStartSelectionAtPoint(it.offset) }
            }
        }

        KeyHandler.getInstance().reset(editor)
    }

    /**
     * This function toggles visual mode.
     *
     * If visual mode is disabled, enable it
     * If visual mode is enabled, but [subMode] differs, update visual according to new [subMode]
     * If visual mode is enabled with the same [subMode], disable it
     */
    fun toggleVisual(editor: Editor, count: Int, rawCount: Int, subMode: CommandState.SubMode): Boolean {
        if (CommandState.getInstance(editor).mode != CommandState.Mode.VISUAL) {
            // Enable visual subMode
            if (rawCount > 0) {
                if (editor.caretModel.caretCount > 1) {
                    // FIXME: 2019-03-05 Support multicaret
                    return false
                }
                // FIXME: 2019-03-05  When there was no previous Visual operation [count] characters are selected.
                val range = editor.caretModel.primaryCaret.vimLastVisualOperatorRange ?: return false
                val newSubMode = range.type.toSubMode()
                val start = editor.caretModel.offset
                val end = calculateVisualRange(editor, range, count)
                val primaryCaret = editor.caretModel.primaryCaret
                CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, newSubMode, MappingMode.VISUAL)
                primaryCaret.vimStartSelectionAtPoint(start)
                MotionGroup.moveCaret(editor, primaryCaret, end)
            } else {
                CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, subMode, MappingMode.VISUAL)
                if (CommandState.inVisualBlockMode(editor)) {
                    editor.caretModel.primaryCaret.let { it.vimStartSelectionAtPoint(it.offset) }
                } else {
                    editor.caretModel.allCarets.forEach { it.vimStartSelectionAtPoint(it.offset) }
                }
            }
            return true
        }

        if (subMode == CommandState.getInstance(editor).subMode) {
            // Disable visual subMode
            VisualMotionGroup.exitVisual(editor)
            return true
        }

        // Update visual subMode with new sub subMode
        CommandState.getInstance(editor).subMode = subMode
        for (caret in editor.caretModel.allCarets) {
            caret.vimUpdateEditorSelection()
        }

        return true
    }

    fun controlNonVimSelectionChange(editor: Editor) {
        if (CommandState.getInstance(editor).mode != CommandState.Mode.VISUAL) {
            if (editor.caretModel.allCarets.any(Caret::hasSelection)) {
                CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER, MappingMode.VISUAL)
                editor.caretModel.allCarets.filter(Caret::hasSelection).forEach { caret ->
                    val start = caret.selectionStart
                    val endAdj = if (exclusiveSelection) 0 else 1
                    val end = (caret.selectionEnd - endAdj).coerceAtLeast(0)
                    caret.vimStartSelectionAtPoint(start)
                    MotionGroup.moveCaret(editor, caret, end)
                }
                KeyHandler.getInstance().reset(editor)
            }
        }
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
        var start = caret.selectionStart
        var end = caret.selectionEnd

        if (CommandState.inVisualBlockMode(editor)) {
            start = caret.vimSelectionStart
            end = caret.offset
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
            SelectionType.CHARACTER_WISE to if (lines > 1) ep.column else ep.column - sp.column
        } else {
            SelectionType.BLOCK_WISE to if (editor.caretModel.primaryCaret.vimLastColumn == MotionGroup.LAST_COLUMN) {
                MotionGroup.LAST_COLUMN
            } else ep.column - sp.column
        }

        return VisualChange(lines, chars, type)
    }

    fun swapVisualEnds(editor: Editor, caret: Caret): Boolean {
        val vimSelectionStart = caret.vimSelectionStart
        caret.vimSelectionStart = caret.offset

        MotionGroup.moveCaret(editor, caret, vimSelectionStart)

        return true
    }

    fun processEscape(editor: Editor) = VisualMotionGroup.exitVisual(editor)

    fun resetVisual(editor: Editor) {
        val wasVisualBlock = CommandState.inVisualBlockMode(editor)
        val selectionType = SelectionType.fromSubMode(CommandState.getInstance(editor).subMode)

        if (!EditorData.isKeepingVisualOperatorAction(editor)) {
            for (caret in editor.caretModel.allCarets) {
                caret.removeSelection()
            }
        }
        if (wasVisualBlock) {
            editor.caretModel.removeSecondaryCarets()
        }

        if (CommandState.inVisualMode(editor)) {
            EditorData.setLastSelectionType(editor, selectionType)
            // FIXME: 2019-03-05 Make it multicaret
            val primaryCaret = editor.caretModel.primaryCaret
            val vimSelectionStart = primaryCaret.vimSelectionStart
            VimPlugin.getMark().setVisualSelectionMarks(editor, TextRange(vimSelectionStart, primaryCaret.offset))
            editor.caretModel.allCarets.forEach { it.vimSelectionStartSetToNull() }

            CommandState.getInstance(editor).subMode = CommandState.SubMode.NONE
        }
    }

    fun exitVisual(editor: Editor) {
        resetVisual(editor)
        if (CommandState.getInstance(editor).mode == CommandState.Mode.VISUAL) {
            CommandState.getInstance(editor).popState()
        }
    }

    val exclusiveSelection: Boolean
        get() = (Options.getInstance().getOption("selection") as BoundStringOption).value == "exclusive"
}
