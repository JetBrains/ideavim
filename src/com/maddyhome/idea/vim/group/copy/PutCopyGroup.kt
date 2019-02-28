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

package com.maddyhome.idea.vim.group.copy

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Ref
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.Register
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MarkGroup
import com.maddyhome.idea.vim.handler.CaretOrder
import com.maddyhome.idea.vim.helper.EditorHelper

object PutCopyGroup {
    fun putVisualRangeCaL(
            editor: Editor,
            context: DataContext,
            caret: Caret,
            range: TextRange,
            count: Int,
            indent: Boolean,
            cursorAfter: Boolean,
            register: Register
    ): Boolean {
        if (range.isMultiple) return false

        val subMode = CommandState.getInstance(editor).subMode
        val updatedRange = if (subMode == CommandState.SubMode.VISUAL_LINE) {
            val fileSize = EditorHelper.getFileSize(editor)
            val end = minOf(range.endOffset + 1, fileSize)
            TextRange(range.startOffset, end)
        } else range

        VimPlugin.getChange().deleteRange(editor, caret, updatedRange, SelectionType.fromSubMode(subMode), false)
        caret.moveToOffset(range.startOffset)

        var startOffset = range.startOffset

        val type = register.type
        if (type == SelectionType.LINE_WISE) {
            if (subMode != CommandState.SubMode.VISUAL_LINE) {
                editor.document.insertString(startOffset, "\n")
                startOffset += 1
            }
        } else if (type == SelectionType.CHARACTER_WISE) {
            if (subMode == CommandState.SubMode.VISUAL_LINE) {
                editor.document.insertString(startOffset, "\n")
            }
        }

        val text = register.text ?: run {
            VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, startOffset)
            VimPlugin.getMark().setChangeMarks(editor, TextRange(startOffset, startOffset))
            return false
        }

        VimPlugin.getCopy().putText(editor, caret, context, text, type, subMode, startOffset,
                count, indent && type == SelectionType.LINE_WISE, cursorAfter)

        return true
    }

    fun putVisualRangeBlockwise(
            editor: Editor,
            context: DataContext,
            ranges: Map<Caret, TextRange>,
            count: Int,
            indent: Boolean,
            cursorAfter: Boolean,
            register: Register,
            insertBefore: Boolean
    ): Boolean {
        val res = Ref.create(true)
        val subMode = CommandState.getInstance(editor).subMode
        val type = register.type
        val lineWiseInsert = type == SelectionType.LINE_WISE

        val linewiseLine = if (insertBefore)
            EditorHelper.getOrderedCaretsList(editor, CaretOrder.INCREASING_OFFSET)[0].visualPosition.line
        else
            EditorHelper.getOrderedCaretsList(editor, CaretOrder.DECREASING_OFFSET)[0].visualPosition.line

        editor.caretModel.runForEachCaret({ caret ->
            val range = ranges[caret] ?: run {
                res.set(false)
                return@runForEachCaret
            }
            VimPlugin.getChange().deleteRange(editor, caret, range, SelectionType.fromSubMode(subMode), false)

            caret.moveToOffset(range.startOffset)

            if (!lineWiseInsert) {
                val startOffset = range.startOffset

                val text = register.text ?: run {
                    VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, startOffset)
                    VimPlugin.getMark().setChangeMarks(editor, TextRange(startOffset, startOffset))
                    res.set(false)
                    return@runForEachCaret
                }

                VimPlugin.getCopy().putText(editor, caret, context, text, type, subMode, startOffset,
                        count, indent && type == SelectionType.LINE_WISE, cursorAfter)
            }

        }, true)

        if (lineWiseInsert) {
            val startOffset = if (insertBefore) {
                EditorHelper.getLineStartOffset(editor, linewiseLine)
            } else {
                EditorHelper.getLineEndOffset(editor, linewiseLine, true)
            }

            var text = register.text ?: run {
                VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, startOffset)
                VimPlugin.getMark().setChangeMarks(editor, TextRange(startOffset, startOffset))
                return false
            }

            if (!insertBefore) text = "\n" + text

            VimPlugin.getCopy().putText(editor, editor.caretModel.primaryCaret, context, text, type, subMode, startOffset,
                    count, indent && type == SelectionType.LINE_WISE, cursorAfter)
        }

        return res.get()
    }
}