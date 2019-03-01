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
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.Register
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MarkGroup
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.handler.CaretOrder.DECREASING_OFFSET
import com.maddyhome.idea.vim.handler.CaretOrder.INCREASING_OFFSET
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.vimTextRange
import java.util.*

object PutCopyGroup {
    fun putVisualRangeCaL(
            editor: Editor,
            context: DataContext,
            caret: Caret,
            range: RangeMarker,
            count: Int,
            indent: Boolean,
            cursorAfter: Boolean,
            register: Register
    ): Boolean {
        val subMode = CommandState.getInstance(editor).subMode
        val updatedRange = if (subMode == CommandState.SubMode.VISUAL_LINE) {
            val fileSize = EditorHelper.getFileSize(editor)
            val end = minOf(range.endOffset + 1, fileSize)
            TextRange(range.startOffset, end)
        } else range.vimTextRange

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

        PutCopyGroup.putText(editor, caret, context, text, type, subMode, startOffset,
                count, indent && type == SelectionType.LINE_WISE, cursorAfter)

        return true
    }

    fun putVisualRangeBlockwise(
            editor: Editor,
            context: DataContext,
            ranges: Map<Caret, RangeMarker>,
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
            EditorHelper.getOrderedCaretsList(editor, INCREASING_OFFSET)[0].visualPosition.line
        else
            EditorHelper.getOrderedCaretsList(editor, DECREASING_OFFSET)[0].visualPosition.line

        editor.caretModel.runForEachCaret({ caret ->
            val range = ranges[caret] ?: run {
                res.set(false)
                return@runForEachCaret
            }
            VimPlugin.getChange().deleteRange(editor, caret, range.vimTextRange, SelectionType.fromSubMode(subMode), false)

            caret.moveToOffset(range.startOffset)

            if (!lineWiseInsert) {
                val startOffset = range.startOffset

                val text = register.text ?: run {
                    VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, startOffset)
                    VimPlugin.getMark().setChangeMarks(editor, TextRange(startOffset, startOffset))
                    res.set(false)
                    return@runForEachCaret
                }

                PutCopyGroup.putText(editor, caret, context, text, type, subMode, startOffset,
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

            PutCopyGroup.putText(editor, editor.caretModel.primaryCaret, context, text, type, subMode, startOffset,
                    count, indent && type == SelectionType.LINE_WISE, cursorAfter)
        }

        return res.get()
    }


    /**
     * Pastes text from the last register into the editor.
     *
     * @param editor  The editor to paste into
     * @param context The data context
     * @param count   The number of times to perform the paste
     * @return true if able to paste, false if not
     */
    fun putText(editor: Editor, context: DataContext, count: Int, indent: Boolean,
                cursorAfter: Boolean, beforeCursor: Boolean): Boolean {
        val register = VimPlugin.getRegister().lastRegister ?: return false
        val selectionType = register.type
        if (selectionType == SelectionType.LINE_WISE && editor.isOneLineMode) return false

        val text = register.text
        val carets = EditorHelper.getOrderedCaretsList(editor, if (beforeCursor) INCREASING_OFFSET else DECREASING_OFFSET)
        for (caret in carets) {
            val startOffset = getStartOffset(editor, caret, selectionType, beforeCursor)

            if (text == null) {
                VimPlugin.getMark().setMark(editor, MarkGroup.MARK_CHANGE_POS, startOffset)
                VimPlugin.getMark().setChangeMarks(editor, TextRange(startOffset, startOffset))
                continue
            }

            PutCopyGroup.putText(editor, caret, context, text, selectionType, CommandState.SubMode.NONE, startOffset, count, indent,
                    cursorAfter)
        }

        return true
    }

    /**
     * This performs the actual insert of the paste
     *
     * @param editor      The editor to paste into
     * @param context     The data context
     * @param startOffset The location within the file to paste the text
     * @param text        The text to paste
     * @param type        The type of paste
     * @param count       The number of times to paste the text
     * @param indent      True if pasted lines should be autoindented, false if not
     * @param cursorAfter If true move cursor to just after pasted text
     * @param mode        The type of highlight prior to the put.
     * @param caret       The caret to insert to
     */
    fun putText(editor: Editor, caret: Caret, context: DataContext, text: String,
                type: SelectionType, mode: CommandState.SubMode, startOffset: Int, count: Int,
                indent: Boolean, cursorAfter: Boolean) {
        var actualText = text
        var actualIndent = indent
        if (mode == CommandState.SubMode.VISUAL_LINE && editor.isOneLineMode) return
        if (actualIndent && type != SelectionType.LINE_WISE && mode != CommandState.SubMode.VISUAL_LINE) actualIndent = false
        if (type == SelectionType.LINE_WISE && actualText.isNotEmpty() && actualText[actualText.length - 1] != '\n') {
            actualText += '\n'
        }

        val endOffset = putTextInternal(editor, caret, context, actualText, type, mode, startOffset, count, actualIndent, cursorAfter)
        VimPlugin.getMark().setChangeMarks(editor, TextRange(startOffset, endOffset))
    }

    private fun putTextInternal(editor: Editor, caret: Caret, context: DataContext,
                                text: String, type: SelectionType, mode: CommandState.SubMode,
                                startOffset: Int, count: Int, indent: Boolean, cursorAfter: Boolean): Int =
            when (type) {
                SelectionType.CHARACTER_WISE -> putTextCharacterwise(editor, caret, context, text, type, mode, startOffset, count, indent, cursorAfter)
                SelectionType.LINE_WISE -> putTextLinewise(editor, caret, context, text, type, mode, startOffset, count, indent, cursorAfter)
                else -> putTextBlockwise(editor, caret, context, text, type, mode, startOffset, count, indent, cursorAfter)
            }

    private fun putTextLinewise(editor: Editor, caret: Caret, context: DataContext,
                                text: String, type: SelectionType, mode: CommandState.SubMode,
                                startOffset: Int, count: Int, indent: Boolean, cursorAfter: Boolean): Int {
        val caretModel = editor.caretModel
        val overlappedCarets = ArrayList<Caret>(caretModel.caretCount)
        for (possiblyOverlappedCaret in caretModel.allCarets) {
            if (possiblyOverlappedCaret.offset != startOffset || possiblyOverlappedCaret === caret) continue

            MotionGroup.moveCaret(editor, possiblyOverlappedCaret,
                    VimPlugin.getMotion().moveCaretHorizontal(editor, possiblyOverlappedCaret, 1, true))
            overlappedCarets.add(possiblyOverlappedCaret)
        }

        val endOffset = putTextCharacterwise(editor, caret, context, text, type, mode, startOffset, count, indent,
                cursorAfter)

        for (overlappedCaret in overlappedCarets) {
            MotionGroup.moveCaret(editor, overlappedCaret,
                    VimPlugin.getMotion().moveCaretHorizontal(editor, overlappedCaret, -1, true))
        }

        return endOffset
    }

    private fun putTextBlockwise(editor: Editor, caret: Caret, context: DataContext,
                                 text: String, type: SelectionType, mode: CommandState.SubMode,
                                 startOffset: Int, count: Int, indent: Boolean, cursorAfter: Boolean): Int {
        val startPosition = editor.offsetToLogicalPosition(startOffset)
        val currentColumn = if (mode == CommandState.SubMode.VISUAL_LINE) 0 else startPosition.column
        var currentLine = startPosition.line

        val lineCount = StringUtil.getLineBreakCount(text) + 1
        if (currentLine + lineCount >= EditorHelper.getLineCount(editor)) {
            val limit = currentLine + lineCount - EditorHelper.getLineCount(editor)
            for (i in 0 until limit) {
                MotionGroup.moveCaret(editor, caret, EditorHelper.getFileSize(editor, true))
                VimPlugin.getChange().insertText(editor, caret, "\n")
            }
        }

        val maxLen = getMaxSegmentLength(text)
        val tokenizer = StringTokenizer(text, "\n")
        var endOffset = startOffset
        while (tokenizer.hasMoreTokens()) {
            var segment = tokenizer.nextToken()
            var origSegment = segment

            if (segment.length < maxLen) {
                segment += " ".repeat(maxLen - segment.length)

                if (currentColumn != 0 && currentColumn < EditorHelper.getLineLength(editor, currentLine)) {
                    origSegment = segment
                }
            }

            val pad = EditorHelper.pad(editor, context, currentLine, currentColumn)

            val insertOffset = editor.logicalPositionToOffset(LogicalPosition(currentLine, currentColumn))
            MotionGroup.moveCaret(editor, caret, insertOffset)
            val insertedText = origSegment + segment.repeat(count - 1)
            VimPlugin.getChange().insertText(editor, caret, insertedText)
            endOffset += insertedText.length

            if (mode == CommandState.SubMode.VISUAL_LINE) {
                MotionGroup.moveCaret(editor, caret, endOffset)
                VimPlugin.getChange().insertText(editor, caret, "\n")
                ++endOffset
            } else {
                if (pad.isNotEmpty()) {
                    MotionGroup.moveCaret(editor, caret, insertOffset)
                    VimPlugin.getChange().insertText(editor, caret, pad)
                    endOffset += pad.length
                }
            }

            ++currentLine
        }

        if (indent) endOffset = doIndent(editor, caret, context, startOffset, endOffset)
        moveCaret(editor, caret, type, mode, startOffset, endOffset, cursorAfter)

        return endOffset
    }

    private fun putTextCharacterwise(editor: Editor, caret: Caret, context: DataContext,
                                     text: String, type: SelectionType,
                                     mode: CommandState.SubMode, startOffset: Int, count: Int, indent: Boolean,
                                     cursorAfter: Boolean): Int {
        MotionGroup.moveCaret(editor, caret, startOffset)
        val insertedText = text.repeat(count)
        VimPlugin.getChange().insertText(editor, caret, insertedText)

        val endOffset = if (indent)
            doIndent(editor, caret, context, startOffset, startOffset + insertedText.length)
        else
            startOffset + insertedText.length
        moveCaret(editor, caret, type, mode, startOffset, endOffset, cursorAfter)

        return endOffset
    }

    private fun getStartOffset(editor: Editor, caret: Caret, type: SelectionType, beforeCursor: Boolean): Int {
        if (beforeCursor) {
            return if (type == SelectionType.LINE_WISE)
                VimPlugin.getMotion().moveCaretToLineStart(editor, caret)
            else
                caret.offset
        }

        var startOffset: Int
        if (type == SelectionType.LINE_WISE) {
            startOffset = Math.min(editor.document.textLength,
                    VimPlugin.getMotion().moveCaretToLineEnd(editor, caret) + 1)
            if (startOffset > 0 && startOffset == editor.document.textLength &&
                    editor.document.charsSequence[startOffset - 1] != '\n') {
                editor.document.insertString(startOffset, "\n")
                startOffset++
            }
        } else {
            startOffset = caret.offset
            if (!EditorHelper.isLineEmpty(editor, caret.logicalPosition.line, false)) {
                startOffset++
            }
        }

        return if (startOffset > 0 && startOffset > editor.document.textLength) startOffset - 1 else startOffset

    }

    private fun moveCaret(editor: Editor, caret: Caret, type: SelectionType,
                          mode: CommandState.SubMode, startOffset: Int, endOffset: Int, cursorAfter: Boolean) {
        val cursorMode = when (type) {
            SelectionType.BLOCK_WISE -> if (mode == CommandState.SubMode.VISUAL_LINE) {
                if (cursorAfter) 4 else 1
            } else {
                if (cursorAfter) 5 else 1
            }
            SelectionType.LINE_WISE -> if (cursorAfter) 4 else 3
            else -> if (mode == CommandState.SubMode.VISUAL_LINE) {
                if (cursorAfter) 4 else 1
            } else {
                if (cursorAfter) 5 else 2
            }
        }

        when (cursorMode) {
            1 -> MotionGroup.moveCaret(editor, caret, startOffset)
            2 -> MotionGroup.moveCaret(editor, caret, endOffset - 1)
            3 -> {
                MotionGroup.moveCaret(editor, caret, startOffset)
                MotionGroup.moveCaret(editor, caret, VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, caret))
            }
            4 -> MotionGroup.moveCaret(editor, caret, endOffset + 1)
            5 -> {
                val pos = Math.min(endOffset, EditorHelper.getLineEndForOffset(editor, endOffset - 1) - 1)
                MotionGroup.moveCaret(editor, caret, pos)
            }
        }
    }

    private fun doIndent(editor: Editor, caret: Caret, context: DataContext, startOffset: Int, endOffset: Int): Int {
        val startLine = editor.offsetToLogicalPosition(startOffset).line
        val endLine = editor.offsetToLogicalPosition(endOffset - 1).line
        val startLineOffset = editor.document.getLineStartOffset(startLine)
        val endLineOffset = editor.document.getLineEndOffset(endLine)

        VimPlugin.getChange().autoIndentRange(editor, caret, context, TextRange(startLineOffset, endLineOffset))
        return EditorHelper.getLineEndOffset(editor, endLine, true)
    }

    private fun getMaxSegmentLength(text: String): Int {
        val tokenizer = StringTokenizer(text, "\n")
        var maxLen = 0
        while (tokenizer.hasMoreTokens()) {
            val s = tokenizer.nextToken()
            maxLen = Math.max(s.length, maxLen)
        }
        return maxLen
    }
}