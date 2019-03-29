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

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.*
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.ChangeGroup
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.helper.*
import com.maddyhome.idea.vim.option.BoundStringOption
import com.maddyhome.idea.vim.option.Options
import java.util.*

/**
 * @author Alex Plate
 */
class VisualMotionGroup {
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
        val autodetectedMode = autodetectVisualMode(editor, mode)

        if (CommandState.inVisualMode(editor)) {
            CommandState.getInstance(editor).popState()
        }
        CommandState.getInstance(editor).pushState(CommandState.Mode.VISUAL, autodetectedMode, MappingMode.VISUAL)
        if (autodetectedMode == CommandState.SubMode.VISUAL_BLOCK) {
            val (start, end) = blockModeStartAndEnd(editor)
            editor.caretModel.removeSecondaryCarets()
            editor.caretModel.primaryCaret.let {
                it.vimStartSelectionAtPoint(start)
                MotionGroup.moveCaret(editor, it, (end - selectionAdj).coerceAtLeast(0))
            }
        } else {
            editor.caretModel.allCarets.forEach {
                if (!it.hasSelection()) {
                    it.vimStartSelectionAtPoint(it.offset)
                    MotionGroup.moveCaret(editor, it, it.offset)
                    return@forEach
                }

                val selectionStart = it.selectionStart
                val selectionEnd = it.selectionEnd
                if (selectionStart == it.offset) {
                    it.vimStartSelectionAtPoint((selectionEnd - selectionAdj).coerceAtLeast(0))
                    MotionGroup.moveCaret(editor, it, selectionStart)
                } else {
                    it.vimStartSelectionAtPoint(selectionStart)
                    MotionGroup.moveCaret(editor, it, (selectionEnd - selectionAdj).coerceAtLeast(0))
                }
            }
        }
        KeyHandler.getInstance().reset(editor)
    }

    private fun autodetectVisualMode(editor: Editor, mode: CommandState.SubMode): CommandState.SubMode {
        if (mode != CommandState.SubMode.NONE) return mode
        if (editor.caretModel.caretCount > 1 && seemsLikeBlockMode(editor)) {
            return CommandState.SubMode.VISUAL_BLOCK
        }
        if (editor.caretModel.allCarets.all { caret ->
                    // Detect if visual mode is character wise or line wise
                    val selectionStart = caret.selectionStart
                    val selectionEnd = caret.selectionEnd
                    val logicalStartLine = editor.offsetToLogicalPosition(selectionStart).line
                    val logicalEnd = editor.offsetToLogicalPosition(selectionEnd)
                    val logicalEndLine = if (logicalEnd.column == 0) (logicalEnd.line - 1).coerceAtLeast(0) else logicalEnd.line
                    val lineStartOfSelectionStart = EditorHelper.getLineStartOffset(editor, logicalStartLine)
                    val lineEndOfSelectionEnd = EditorHelper.getLineEndOffset(editor, logicalEndLine, true)
                    lineStartOfSelectionStart == selectionStart && (lineEndOfSelectionEnd + 1 == selectionEnd || lineEndOfSelectionEnd == selectionEnd)
                }) return CommandState.SubMode.VISUAL_LINE
        return CommandState.SubMode.VISUAL_CHARACTER
    }

    private fun seemsLikeBlockMode(editor: Editor): Boolean {
        val selections = editor.caretModel.allCarets.map {
            val adj = if (editor.offsetToLogicalPosition(it.selectionEnd).column == 0) 1 else 0
            it.selectionStart to (it.selectionEnd - adj).coerceAtLeast(0)
        }.sortedBy { it.first }
        val selectionStartColumn = editor.offsetToLogicalPosition(selections.first().first).column
        val selectionStartLine = editor.offsetToLogicalPosition(selections.first().first).line

        val maxColumn = selections.map { editor.offsetToLogicalPosition(it.second).column }.max() ?: return false
        selections.forEachIndexed { i, it ->
            if (editor.offsetToLogicalPosition(it.first).line != editor.offsetToLogicalPosition(it.second).line) {
                return false
            }
            if (editor.offsetToLogicalPosition(it.first).column != selectionStartColumn) {
                return false
            }
            val lineEnd = editor.offsetToLogicalPosition(EditorHelper.getLineEndForOffset(editor, it.second)).column
            if (editor.offsetToLogicalPosition(it.second).column != maxColumn.coerceAtMost(lineEnd)) {
                return false
            }
            if (editor.offsetToLogicalPosition(it.first).line != selectionStartLine + i) {
                return false
            }
        }
        return true
    }

    private fun blockModeStartAndEnd(editor: Editor): Pair<Int, Int> {
        val selections = editor.caretModel.allCarets.map { it.selectionStart to it.selectionEnd }.sortedBy { it.first }
        val maxColumn = selections.map { editor.offsetToLogicalPosition(it.second).column }.max()
                ?: throw RuntimeException("No carets")
        val lastLine = editor.offsetToLogicalPosition(selections.last().first).line
        return selections.first().first to editor.logicalPositionToOffset(LogicalPosition(lastLine, maxColumn))
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
            exitVisual(editor)
            return true
        }

        // Update visual subMode with new sub subMode
        CommandState.getInstance(editor).subMode = subMode
        for (caret in editor.caretModel.allCarets) {
            caret.vimUpdateEditorSelection()
        }

        return true
    }

    fun enterSelectionMode(editor: Editor, subMode: CommandState.SubMode): Boolean {
        CommandState.getInstance(editor).pushState(CommandState.Mode.SELECT, subMode, MappingMode.SELECT)
        editor.caretModel.allCarets.forEach {
            it.run {
                vimSelectionStart = if (subMode == CommandState.SubMode.VISUAL_LINE) offset else leadSelectionOffset
            }
        }
        KeyHandler.getInstance().reset(editor)
        ChangeGroup.resetCursor(editor, true)
        return true
    }

    fun exitSelectMode(editor: Editor) {
        if (!CommandState.inSelectMode(editor)) return

        CommandState.getInstance(editor).popState()
        val primaryCaret = editor.caretModel.primaryCaret
        primaryCaret.removeSelection()
        primaryCaret.vimSelectionStartSetToNull()
        ChangeGroup.resetCursor(editor, false)
    }

    fun controlNonVimSelectionChange(editor: Editor, resetCaretToInsert: Boolean = false) {
        if (editor.caretModel.allCarets.any(Caret::hasSelection)) {
            val commandState = CommandState.getInstance(editor)
            while (commandState.mode != CommandState.Mode.COMMAND) {
                commandState.popState()
            }
            val autodetectedMode = autodetectVisualMode(editor, CommandState.SubMode.NONE)
            while (CommandState.getInstance(editor).mode != CommandState.Mode.COMMAND) {
                CommandState.getInstance(editor).popState()
            }
            enterSelectionMode(editor, autodetectedMode)
        } else {
            ChangeGroup.resetCursor(editor, resetCaretToInsert)
            exitVisual(editor)
            exitSelectMode(editor)
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

    fun resetVisual(editor: Editor) {
        val wasVisualBlock = CommandState.inVisualBlockMode(editor)
        val selectionType = SelectionType.fromSubMode(CommandState.getInstance(editor).subMode)

        SelectionVimListenerSuppressor.lock()
        if (wasVisualBlock) {
            editor.caretModel.allCarets.forEach { it.visualAttributes = editor.caretModel.primaryCaret.visualAttributes }
            editor.caretModel.removeSecondaryCarets()
        }
        if (!EditorData.isKeepingVisualOperatorAction(editor)) {
            editor.caretModel.allCarets.forEach(Caret::removeSelection)
        }
        SelectionVimListenerSuppressor.unlock()

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
    val selectionAdj: Int
        get() = if (exclusiveSelection) 0 else 1
}
