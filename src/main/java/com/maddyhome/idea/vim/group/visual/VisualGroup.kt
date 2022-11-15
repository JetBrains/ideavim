/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.visual

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.editorMode
import com.maddyhome.idea.vim.helper.inBlockSubMode
import com.maddyhome.idea.vim.helper.inSelectMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.isEndAllowed
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.helper.subMode
import com.maddyhome.idea.vim.helper.updateCaretsVisualAttributes
import com.maddyhome.idea.vim.helper.vimLastColumn
import com.maddyhome.idea.vim.helper.vimSelectionStart
import com.maddyhome.idea.vim.newapi.IjVimEditor

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
  if (moveCaretToSelectionEnd && !editor.inBlockSubMode) moveToInlayAwareOffset(end)
}

/**
 * Move selection end to current caret position
 * This method is created only for Character and Line mode
 * @see vimMoveBlockSelectionToOffset for blockwise selection
 */
fun Caret.vimMoveSelectionToCaret() {
  if (!editor.inVisualMode && !editor.inSelectMode) error("Attempt to extent selection in non-visual mode")
  if (editor.inBlockSubMode) error("Move caret with [vimMoveBlockSelectionToOffset]")

  val startOffsetMark = vimSelectionStart

  setVisualSelection(startOffsetMark, offset, this)
}

/**
 * Move selection end to current primary caret position
 *
 * This method is created only for block mode. Note that this method will invalidate all carets!
 *
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

      return if (editor.subMode == VimStateMachine.SubMode.VISUAL_LINE) {
        val selectionStartLine = editor.offsetToLogicalPosition(selectionStart).line
        val caretLine = editor.offsetToLogicalPosition(this.offset).line
        if (caretLine == selectionStartLine) {
          val column = editor.offsetToLogicalPosition(selectionEnd).column
          if (column == 0) (selectionEnd - 1).coerceAtLeast(0) else selectionEnd
        } else selectionStart
      } else if (editor.inBlockSubMode) {
        val selections = editor.caretModel.allCarets.map { it.selectionStart to it.selectionEnd }.sortedBy { it.first }
        val pCaret = editor.caretModel.primaryCaret
        when (pCaret.offset) {
          selections.first().first -> (selections.last().second - selectionAdj).coerceAtLeast(0)
          selections.first().second -> selections.last().first
          selections.last().first -> (selections.first().second - selectionAdj).coerceAtLeast(0)
          selections.last().second -> selections.first().first
          else -> selections.first().first
        }
      } else {
        if (caretOffset == selectionStart) (selectionEnd - selectionAdj).coerceAtLeast(0) else selectionStart
      }
    }
    return caretOffset
  }

fun moveCaretOneCharLeftFromSelectionEnd(editor: Editor, predictedMode: VimStateMachine.Mode) {
  if (predictedMode != VimStateMachine.Mode.VISUAL) {
    if (!predictedMode.isEndAllowed) {
      editor.caretModel.allCarets.forEach { caret ->
        val lineEnd = EditorHelper.getLineEndForOffset(editor, caret.offset)
        val lineStart = EditorHelper.getLineStartForOffset(editor, caret.offset)
        if (caret.offset == lineEnd && lineEnd != lineStart) caret.moveToInlayAwareOffset(caret.offset - 1)
      }
    }
    return
  }
  editor.caretModel.allCarets.forEach { caret ->
    if (caret.hasSelection() && caret.selectionEnd == caret.offset) {
      if (caret.selectionEnd <= 0) return@forEach
      if (EditorHelper.getLineStartForOffset(editor, caret.selectionEnd - 1) != caret.selectionEnd - 1 &&
        caret.selectionEnd > 1 && editor.document.text[caret.selectionEnd - 1] == '\n'
      ) {
        caret.moveToInlayAwareOffset(caret.selectionEnd - 2)
      } else {
        caret.moveToInlayAwareOffset(caret.selectionEnd - 1)
      }
    }
  }
}

private fun setVisualSelection(selectionStart: Int, selectionEnd: Int, caret: Caret) {
  val (start, end) = if (selectionStart > selectionEnd) selectionEnd to selectionStart else selectionStart to selectionEnd
  val editor = caret.editor
  val subMode = editor.subMode
  val mode = editor.editorMode
  val vimEditor = IjVimEditor(editor)
  when (subMode) {
    VimStateMachine.SubMode.VISUAL_CHARACTER -> {
      val (nativeStart, nativeEnd) = charToNativeSelection(vimEditor, start, end, mode)
      caret.vimSetSystemSelectionSilently(nativeStart, nativeEnd)
    }
    VimStateMachine.SubMode.VISUAL_LINE -> {
      val (nativeStart, nativeEnd) = lineToNativeSelection(vimEditor, start, end)
      caret.vimSetSystemSelectionSilently(nativeStart, nativeEnd)
    }
    VimStateMachine.SubMode.VISUAL_BLOCK -> {
      // This will invalidate any secondary carets, but we shouldn't have any of these cached in local variables, etc.
      editor.caretModel.removeSecondaryCarets()

      // Set system selection
      val (blockStart, blockEnd) = blockToNativeSelection(vimEditor, selectionStart, selectionEnd, mode)
      val lastColumn = editor.caretModel.primaryCaret.vimLastColumn

      // WARNING! This can invalidate the primary caret! I.e. the `caret` parameter will no longer be the primary caret.
      // Given an existing visual block selection, moving the caret will first remove all secondary carets (above) then
      // this method will ask IntelliJ to create a new multi-caret block selection. If we're moving up (`k`) a new caret
      // is added, and becomes the new primary caret. The current `caret` parameter remains valid, but is no longer the
      // primary caret. Make sure to fetch the new primary caret if necessary.
      vimEditor.vimSetSystemBlockSelectionSilently(blockStart, blockEnd)

      // We've just added secondary carets again, hide them to better emulate block selection
      editor.updateCaretsVisualAttributes()

      for (aCaret in editor.caretModel.allCarets) {
        if (!aCaret.isValid) continue
        val line = aCaret.logicalPosition.line
        val lineEndOffset = EditorHelper.getLineEndOffset(editor, line, true)
        val lineStartOffset = EditorHelper.getLineStartOffset(editor, line)

        // Extend selection to line end if it was made with `$` command
        if (lastColumn >= VimMotionGroupBase.LAST_COLUMN) {
          aCaret.vimSetSystemSelectionSilently(aCaret.selectionStart, lineEndOffset)
          val newOffset = (lineEndOffset - VimPlugin.getVisualMotion().selectionAdj).coerceAtLeast(lineStartOffset)
          aCaret.moveToInlayAwareOffset(newOffset)
        }
        val visualPosition = editor.offsetToVisualPosition(aCaret.selectionEnd)
        if (aCaret.offset == aCaret.selectionEnd && visualPosition != aCaret.visualPosition) {
          // Put right caret position for tab character
          aCaret.moveToVisualPosition(visualPosition)
        }
        if (mode != VimStateMachine.Mode.SELECT &&
          !EditorHelper.isLineEmpty(editor, line, false) &&
          aCaret.offset == aCaret.selectionEnd &&
          aCaret.selectionEnd - 1 >= lineStartOffset &&
          aCaret.selectionEnd - aCaret.selectionStart != 0
        ) {
          // Move all carets one char left in case if it's on selection end
          aCaret.moveToVisualPosition(VisualPosition(visualPosition.line, visualPosition.column - 1))
        }
      }

      editor.caretModel.primaryCaret.moveToInlayAwareOffset(selectionEnd)
    }
    else -> Unit
  }
}
