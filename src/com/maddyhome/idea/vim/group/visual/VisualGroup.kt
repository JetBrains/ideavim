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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretVisualAttributes
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.group.ChangeGroup
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.inSelectMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.mode
import com.maddyhome.idea.vim.helper.sort
import com.maddyhome.idea.vim.helper.subMode
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.helper.vimSelectionStart

/**
 * @author Alex Plate
 */

/**
 * Set selection for caret
 * This method doesn't change CommandState and operates only with caret and it's properties
 * if [moveCaretToSelectionEnd] is true, caret movement to [end] will be performed
 */
fun Caret.vimSetSelection(start: Int, end: Int = start, moveCaretToSelectionEnd: Boolean = false) {
  vimSelectionStart = start
  setVisualSelection(start, end, this)
  if (moveCaretToSelectionEnd && !editor.inBlockSubMode) moveToOffset(end)
}

/**
 * Move selection end to current caret position
 * This method is created only for Character and Line mode
 * @see vimMoveBlockSelectionToOffset for blockwise selection
 */
fun Caret.vimMoveSelectionToCaret() {
  if (!editor.inVisualMode && !editor.inSelectMode)
    throw RuntimeException("Attempt to extent selection in non-visual mode")
  if (editor.inBlockSubMode)
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
 * This works almost like [Caret.getLeadSelectionOffset], but vim-specific
 */
val Caret.vimLeadSelectionOffset: Int
  get() {
    val caretOffset = offset
    if (hasSelection()) {
      val selectionAdj = VimPlugin.getVisualMotion().selectionAdj
      if (caretOffset != selectionStart && caretOffset != selectionEnd) {
        // Try to check if current selection is tweaked by fold region.
        val foldingModel = editor.foldingModel
        val foldRegion = foldingModel.getCollapsedRegionAtOffset(caretOffset)
        if (foldRegion != null) {
          if (foldRegion.startOffset == selectionStart) {
            return (selectionEnd - selectionAdj).coerceAtLeast(0)
          } else if (foldRegion.endOffset == selectionEnd) {
            return selectionStart
          }
        }
      }

      return if (editor.subMode == CommandState.SubMode.VISUAL_LINE) {
        val selectionStartLine = editor.offsetToLogicalPosition(selectionStart).line
        val caretLine = editor.offsetToLogicalPosition(this.offset).line
        if (caretLine == selectionStartLine) {
          val column = editor.offsetToLogicalPosition(selectionEnd).column
          if (column == 0) (selectionEnd - 1).coerceAtLeast(0) else selectionEnd
        } else selectionStart
      } else if (editor.inBlockSubMode) {
        val selections = editor.caretModel.allCarets.map { it.selectionStart to it.selectionEnd }.sortedBy { it.first }
        val pCaret = editor.caretModel.primaryCaret
        when {
          pCaret.offset == selections.first().first -> (selections.last().second - selectionAdj).coerceAtLeast(0)
          pCaret.offset == selections.first().second -> selections.last().first
          pCaret.offset == selections.last().first -> (selections.first().second - selectionAdj).coerceAtLeast(0)
          pCaret.offset == selections.last().second -> selections.first().first
          else -> selections.first().first
        }
      } else {
        if (caretOffset == selectionStart) (selectionEnd - selectionAdj).coerceAtLeast(0) else selectionStart
      }
    }
    return caretOffset
  }

/**
 * Update caret's colour according to the current state
 *
 * Secondary carets became invisible colour in visual block mode
 */
fun updateCaretState(editor: Editor) {
  // Update colour
  if (editor.inBlockSubMode) {
    editor.caretModel.allCarets.forEach {
      if (it != editor.caretModel.primaryCaret) {
        // Set background color for non-primary carets as selection background color
        //   to make them invisible
        val color = editor.colorsScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR)
        val visualAttributes = it.visualAttributes
        it.visualAttributes = CaretVisualAttributes(color, visualAttributes.weight)
      }
    }
  } else {
    editor.caretModel.allCarets.forEach { it.visualAttributes = CaretVisualAttributes.DEFAULT }
  }

  // Update shape
  editor.mode.resetShape(editor)
}

fun CommandState.Mode.resetShape(editor: Editor) = when (this) {
  CommandState.Mode.COMMAND, CommandState.Mode.VISUAL, CommandState.Mode.REPLACE -> ChangeGroup.resetCaret(editor, false)
  CommandState.Mode.SELECT, CommandState.Mode.INSERT -> ChangeGroup.resetCaret(editor, true)
  CommandState.Mode.CMD_LINE -> Unit
}

fun charToNativeSelection(editor: Editor, start: Int, end: Int, mode: CommandState.Mode): Pair<Int, Int> {
  val (nativeStart, nativeEnd) = sort(start, end)
  val lineEnd = EditorHelper.getLineEndForOffset(editor, nativeEnd)
  val adj = if (VimPlugin.getVisualMotion().exclusiveSelection || nativeEnd == lineEnd || mode == CommandState.Mode.SELECT) 0 else 1
  val adjEnd = (nativeEnd + adj).coerceAtMost(EditorHelper.getFileSize(editor))
  return nativeStart to adjEnd
}

/**
 * Convert vim's selection start and end to corresponding native selection.
 *
 * Adds caret adjustment or extends to line start / end in case of linewise selection
 */
fun lineToNativeSelection(editor: Editor, start: Int, end: Int): Pair<Int, Int> {
  val (nativeStart, nativeEnd) = sort(start, end)
  val lineStart = EditorHelper.getLineStartForOffset(editor, nativeStart)
  // Extend to \n char of line to fill full line with selection
  val lineEnd = (EditorHelper.getLineEndForOffset(editor, nativeEnd) + 1).coerceAtMost(EditorHelper.getFileSize(editor, true))
  return lineStart to lineEnd
}

fun blockToNativeSelection(editor: Editor, start: Int, end: Int, mode: CommandState.Mode): Pair<LogicalPosition, LogicalPosition> {
  var blockStart = editor.offsetToLogicalPosition(start)
  var blockEnd = editor.offsetToLogicalPosition(end)
  if (!VimPlugin.getVisualMotion().exclusiveSelection && mode != CommandState.Mode.SELECT) {
    if (blockStart.column > blockEnd.column) {
      if (blockStart.column < EditorHelper.getLineLength(editor, blockStart.line)) {
        blockStart = LogicalPosition(blockStart.line, blockStart.column + 1)
      }
    } else {
      if (blockEnd.column < EditorHelper.getLineLength(editor, blockEnd.line)) {
        blockEnd = LogicalPosition(blockEnd.line, blockEnd.column + 1)
      }
    }
  }
  return blockStart to blockEnd
}

fun moveCaretOneCharLeftFromSelectionEnd(editor: Editor, predictedMode: CommandState.Mode) {
  predictedMode.resetShape(editor)
  if (predictedMode != CommandState.Mode.VISUAL) {
    if (!predictedMode.isEndAllowed) {
      editor.caretModel.allCarets.forEach { caret ->
        val lineEnd = EditorHelper.getLineEndForOffset(editor, caret.offset)
        val lineStart = EditorHelper.getLineStartForOffset(editor, caret.offset)
        if (caret.offset == lineEnd && lineEnd != lineStart) caret.moveToOffset(caret.offset - 1)
      }
    }
    return
  }
  editor.caretModel.allCarets.forEach { caret ->
    if (caret.hasSelection() && caret.selectionEnd == caret.offset) {
      if (caret.selectionEnd <= 0) return@forEach
      if (EditorHelper.getLineStartForOffset(editor, caret.selectionEnd - 1) != caret.selectionEnd - 1
        && caret.selectionEnd > 1 && editor.document.text[caret.selectionEnd - 1] == '\n') {
        caret.moveToOffset(caret.selectionEnd - 2)
      } else {
        caret.moveToOffset(caret.selectionEnd - 1)
      }
    }
  }
}

private fun setVisualSelection(selectionStart: Int, selectionEnd: Int, caret: Caret) {
  val (start, end) = if (selectionStart > selectionEnd) selectionEnd to selectionStart else selectionStart to selectionEnd
  val editor = caret.editor
  val subMode = editor.subMode
  val mode = editor.mode
  when (subMode) {
    CommandState.SubMode.VISUAL_CHARACTER -> {
      val (nativeStart, nativeEnd) = charToNativeSelection(editor, start, end, mode)
      caret.vimSetSystemSelectionSilently(nativeStart, nativeEnd)
    }
    CommandState.SubMode.VISUAL_LINE -> {
      val (nativeStart, nativeEnd) = lineToNativeSelection(editor, start, end)
      caret.vimSetSystemSelectionSilently(nativeStart, nativeEnd)
    }
    CommandState.SubMode.VISUAL_BLOCK -> {
      editor.caretModel.removeSecondaryCarets()

      // Set system selection
      val (blockStart, blockEnd) = blockToNativeSelection(editor, selectionStart, selectionEnd, mode)
      val lastColumn = editor.caretModel.primaryCaret.vimLastColumn
      editor.selectionModel.vimSetSystemBlockSelectionSilently(blockStart, blockEnd)

      for (aCaret in editor.caretModel.allCarets) {
        if (!aCaret.isValid) continue
        val line = aCaret.logicalPosition.line
        val lineEndOffset = EditorHelper.getLineEndOffset(editor, line, true)
        val lineStartOffset = EditorHelper.getLineStartOffset(editor, line)

        // Extend selection to line end if it was made with `$` command
        if (lastColumn >= MotionGroup.LAST_COLUMN) {
          aCaret.vimSetSystemSelectionSilently(aCaret.selectionStart, lineEndOffset)
          val newOffset = (lineEndOffset - VimPlugin.getVisualMotion().selectionAdj).coerceAtLeast(lineStartOffset)
          aCaret.moveToOffset(newOffset)
        }
        val visualPosition = editor.offsetToVisualPosition(aCaret.selectionEnd)
        if (aCaret.offset == aCaret.selectionEnd && visualPosition != aCaret.visualPosition) {
          // Put right caret position for tab character
          aCaret.moveToVisualPosition(visualPosition)
        }
        if (mode != CommandState.Mode.SELECT
          && !EditorHelper.isLineEmpty(editor, line, false)
          && aCaret.offset == aCaret.selectionEnd
          && aCaret.selectionEnd - 1 >= lineStartOffset
          && aCaret.selectionEnd - aCaret.selectionStart != 0) {
          // Move all carets one char left in case if it's on selection end
          aCaret.moveToVisualPosition(VisualPosition(visualPosition.line, visualPosition.column - 1))
        }
      }

      editor.caretModel.primaryCaret.moveToOffset(selectionEnd)
    }
    else -> Unit
  }
  updateCaretState(editor)
}
