/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.codeInsight.editorActions.EnterHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.psi.util.PsiUtilBase
import com.intellij.util.text.CharArrayUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.IndentConfig
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.ChangeGroup
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.commandState
import com.maddyhome.idea.vim.helper.inlayAwareVisualColumn
import com.maddyhome.idea.vim.helper.vimCarets
import com.maddyhome.idea.vim.helper.vimChangeActionSwitchMode
import com.maddyhome.idea.vim.helper.vimLastColumn
import kotlin.math.min

fun changeRange(
  editor: Editor,
  caret: Caret,
  range: TextRange,
  type: SelectionType,
  context: DataContext,
) {
  val vimEditor = IjVimEditor(editor)
  val vimRange = toVimRange(range, type)

  var col = 0
  var lines = 0
  if (type === SelectionType.BLOCK_WISE) {
    lines = ChangeGroup.getLinesCountInVisualBlock(editor, range)
    col = editor.offsetToLogicalPosition(range.startOffset).column
    if (caret.vimLastColumn == MotionGroup.LAST_COLUMN) {
      col = MotionGroup.LAST_COLUMN
    }
  }

  // Remove the range
  val vimCaret = IjVimCaret(caret)
  val indent = editor.offsetToLogicalPosition(vimEditor.indentForLine(vimCaret.getLine().line)).column
  val deletedInfo = VimMachine.instance.delete(vimRange, vimEditor, vimCaret)
  if (deletedInfo != null) {
    if (deletedInfo is OperatedRange.Lines) {
      // Add new line in case of linewise motion
      val existingLine = if (vimEditor.fileSize() != 0L) {
        if (deletedInfo.shiftType != LineDeleteShift.NO_NL) {
          vimEditor.addLine(deletedInfo.lineAbove)
        } else {
          EditorLine.Pointer.init(deletedInfo.lineAbove.line, vimEditor)
        }
      } else {
        EditorLine.Pointer.init(0, vimEditor)
      }

      val offset = vimCaret.offsetForLineWithStartOfLineOption(existingLine)
      // TODO: 29.12.2021 IndentConfig is not abstract
      val indentText = IndentConfig.create(editor).createIndentBySize(indent)
      vimEditor.insertText(offset.offset, indentText)
      val caretOffset = offset + indentText.length
      vimCaret.moveToOffset(caretOffset)
      VimPlugin.getChange().insertBeforeCursor(editor, context)
    } else {
      when (deletedInfo) {
        is OperatedRange.Characters -> {
          vimCaret.moveToOffset(deletedInfo.leftOffset.point)
        }
        is OperatedRange.Block -> TODO()
      }
      if (type == SelectionType.BLOCK_WISE) {
        VimPlugin.getChange().setInsertRepeat(lines, col, false)
      }
      editor.vimChangeActionSwitchMode = CommandState.Mode.INSERT
    }
  } else {
    VimPlugin.getChange().insertBeforeCursor(editor, context)
  }
}

fun deleteRange(
  editor: Editor,
  caret: Caret,
  range: TextRange,
  type: SelectionType,
): Boolean {
  val vimEditor = IjVimEditor(editor)
  val vimRange = toVimRange(range, type)

  val vimCaret = IjVimCaret(caret)
  vimCaret.caret.vimLastColumn = vimCaret.caret.inlayAwareVisualColumn
  val deletedInfo = VimMachine.instance.delete(vimRange, vimEditor, vimCaret)
  if (deletedInfo != null) {
    when (deletedInfo) {
      is OperatedRange.Characters -> {
        val newOffset = EditorHelper.normalizeOffset(editor, deletedInfo.leftOffset.point, false)
        vimCaret.moveToOffset(newOffset)
      }
      is OperatedRange.Block -> TODO()
      is OperatedRange.Lines -> {
        if (deletedInfo.shiftType != LineDeleteShift.NL_ON_START) {
          val line = deletedInfo.lineAbove.toPointer(vimEditor)
          val offset = vimCaret.offsetForLineWithStartOfLineOption(line)
          vimCaret.moveToOffset(offset)
        } else {
          val logicalLine = EditorLine.Pointer.init((deletedInfo.lineAbove.line - 1).coerceAtLeast(0), vimEditor)
          val offset = vimCaret.offsetForLineWithStartOfLineOption(logicalLine)
          vimCaret.moveToOffset(offset)
        }
      }
    }
  }
  return deletedInfo != null
}

fun insertLineAround(editor: Editor, context: DataContext, shift: Int) {
  val vimEditor: MutableVimEditor = IjVimEditor(editor)
  val project = editor.project

  VimPlugin.getChange().initInsert(editor, context, CommandState.Mode.INSERT)

  if (!editor.commandState.isDotRepeatInProgress) {
    for (caret in editor.vimCarets()) {
      val vimCaret: VimCaret = IjVimCaret(caret)
      val line = vimCaret.getLine()

      // Calculating next line with minding folders
      val lineEndOffset = if (shift == 1) {
        VimPlugin.getMotion().moveCaretToLineEnd(editor, caret)
      } else {
        VimPlugin.getMotion().moveCaretToLineStart(editor, caret)
      }
      val position = EditorLine.Offset.init(editor.offsetToLogicalPosition(lineEndOffset).line + shift, vimEditor)

      val insertedLine = vimEditor.addLine(position) ?: continue
      VimPlugin.getChange().saveStrokes("\n")

      var lineStart = vimEditor.getLineRange(insertedLine).first
      val initialLineStart = lineStart

      // Set up indent
      // Firstly set up primitive indent
      val lineStartOffset = vimEditor.getLineRange(line).first.point + (1 - shift)
      val text = editor.document.charsSequence
      val lineStartWsEndOffset = CharArrayUtil.shiftForward(text, lineStartOffset, " \t")
      val indent = text.subSequence(lineStartOffset, min(caret.offset, lineStartWsEndOffset))
      vimEditor.insertText(lineStart, indent)
      lineStart = (lineStart.point + indent.length).offset

      if (project != null) {
        // Secondly set up language smart indent
        val language = PsiUtilBase.getLanguageInEditor(caret, project)
        val newIndent = EnterHandler.adjustLineIndentNoCommit(language, editor.document, editor, lineStart.point)
        lineStart = if (newIndent >= 0) newIndent.offset else lineStart
      }
      VimPlugin.getChange()
        .saveStrokes(
          editor.document.getText(
            com.intellij.openapi.util.TextRange(
              initialLineStart.point,
              lineStart.point
            )
          )
        )

      vimCaret.moveToOffset(lineStart.point)
    }
  }

  MotionGroup.scrollCaretIntoView(editor)
}
