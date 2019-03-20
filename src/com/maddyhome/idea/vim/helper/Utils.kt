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

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.group.motion.VisualMotionGroup
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

val RangeMarker.vimTextRange: TextRange
    get() = TextRange(startOffset, endOffset)

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

    val vimBlockMainCaret = editor.vimBlockMainCaret
    val startOffsetMark = vimBlockMainCaret.vimSelectionStart

    setVisualSelection(startOffsetMark, offset, vimBlockMainCaret)
}

fun Caret.vimUpdateEditorSelection() {
    val startOffsetMark = vimSelectionStart
    setVisualSelection(startOffsetMark, offset, this)
}

private fun setVisualSelection(firstOffset: Int, secondOffset: Int, caret: Caret) {
    val (start, end) = if (firstOffset > secondOffset) secondOffset to firstOffset else firstOffset to secondOffset
    val editor = caret.editor
    val subMode = CommandState.getInstance(editor).subMode
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

            var blockStart = editor.offsetToLogicalPosition(start)
            var blockEnd = editor.offsetToLogicalPosition(end)
            if (blockStart.column > blockEnd.column) {
                // swap variables
                blockStart = blockEnd.also { blockEnd = blockStart }
            }
            if (!VisualMotionGroup.exclusiveSelection) {
                blockEnd = LogicalPosition(blockEnd.line, blockEnd.column + 1)
            }
            editor.selectionModel.setBlockSelection(blockStart, blockEnd)

            for (aCaret in editor.caretModel.allCarets) {
                val line = aCaret.logicalPosition.line
                val lineEndOffset = EditorHelper.getLineEndOffset(editor, line, true)

                if (CaretData.getLastColumn(editor.caretModel.primaryCaret) >= MotionGroup.LAST_COLUMN) {
                    aCaret.setSelection(aCaret.selectionStart, lineEndOffset)
                }
                if (!EditorHelper.isLineEmpty(editor, line, false)) {
                    aCaret.moveToOffset(aCaret.selectionEnd - 1)
                }
            }

            val startLine = editor.offsetToLogicalPosition(caret.vimSelectionStart).line
            val startColumn = editor.offsetToLogicalPosition(caret.vimSelectionStart).column
            if (editor.caretModel.allCarets.first().logicalPosition.line == startLine) {
                editor.vimBlockMainCaret = editor.caretModel.allCarets.last()
            } else {
                editor.vimBlockMainCaret = editor.caretModel.allCarets.first()
            }
            if (editor.vimBlockMainCaret.logicalPosition.column <= startColumn) {
                editor.vimBlockMainCaret.moveToOffset(editor.vimBlockMainCaret.selectionStart)
            }
        }
    }
}

@Target(AnnotationTarget.FUNCTION)
annotation class VimBehaviourDiffers(
        val originalVimAfter: String = "",
        val trimIndent: Boolean = false,
        val description: String = ""
)

fun <T> userData(): ReadWriteProperty<UserDataHolder, T?> {
    return object : ReadWriteProperty<UserDataHolder, T?> {
        private var key: Key<T>? = null
        private fun getKey(property: KProperty<*>): Key<T> {
            if (key == null) {
                key = Key.create(property.name + "by userData()")
            }
            return key as Key<T>
        }

        override fun getValue(thisRef: UserDataHolder, property: KProperty<*>): T? {
            return thisRef.getUserData(getKey(property))
        }

        override fun setValue(thisRef: UserDataHolder, property: KProperty<*>, value: T?) {
            thisRef.putUserData(getKey(property), value)
        }
    }
}
