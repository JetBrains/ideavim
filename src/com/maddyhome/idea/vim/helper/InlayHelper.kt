/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.helper

import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.VisualPosition

/**
 * Move the caret to the given offset, handling inline inlays
 *
 * Inline inlays take up a single visual column. The caret can be positioned on the visual column of the inlay or the
 * text. For Vim, we always want to position the caret before the text (when rendered as a block, this means over the
 * text, and not over the inlay). Caret.moveToOffset will position itself correctly when an inlay relates to following
 * text - it correctly adds one to the visual column. However, it does not add one if the inlay relates to preceding
 * text.
 *
 * I believe this is an incorrect implementation of EditorUtil.inlayAwareOffsetToVisualPosition. When adding an
 * inlay, it is added at an offset, and a new visual column is inserted there. When moving to an offset, that visual
 * column is always there, regardless of whether the inlay relates to preceding or following text.
 *
 * It is safe to call this method if the caret hasn't actually moved. In fact, it is a good idea to do so, as it will
 * make sure that if the document has changed to place an inlay at the caret position, the caret is re-positioned
 * appropriately
 */
fun Caret.moveToInlayAwareOffset(offset: Int) {
  // If the target offset is collapsed inside a fold, move directly to the offset, expanding the fold
  if (editor.foldingModel.isOffsetCollapsed(offset)) {
    moveToOffset(offset)
  } else {
    val newVisualPosition = inlayAwareOffsetToVisualPosition(editor, offset)
    if (newVisualPosition != visualPosition) {
      moveToVisualPosition(newVisualPosition)
    }
  }
}

fun Caret.moveToInlayAwareLogicalPosition(pos: LogicalPosition) {
  moveToInlayAwareOffset(editor.logicalPositionToOffset(pos))
}

// This is the same as EditorUtil.inlayAwareOffsetToVisualPosition, except it always skips the inlay, regardless of
// its "relates to preceding text" state
private fun inlayAwareOffsetToVisualPosition(editor: Editor, offset: Int): VisualPosition {
  var logicalPosition = editor.offsetToLogicalPosition(offset)
  val e = if (editor is EditorWindow) {
    logicalPosition = editor.injectedToHost(logicalPosition)
    editor.delegate
  } else {
    editor
  }
  var pos = e.logicalToVisualPosition(logicalPosition)
  while (editor.inlayModel.getInlineElementAt(pos) != null) {
    pos = VisualPosition(pos.line, pos.column + 1)
  }
  return pos
}

val Caret.inlayAwareVisualColumn: Int
  get() = this.visualPosition.column - this.amountOfInlaysBeforeCaret

val Caret.amountOfInlaysBeforeCaret: Int
  get() {
    val curLineStartOffset: Int = this.editor.document.getLineStartOffset(logicalPosition.line)
    return this.editor.inlayModel.getInlineElementsInRange(curLineStartOffset, this.offset).size
  }

fun Editor.amountOfInlaysBeforeVisualPosition(pos: VisualPosition): Int {
  val newOffset = EditorHelper.visualPositionToOffset(this, pos)
  val lineStartNewOffset: Int = this.document.getLineStartOffset(this.visualToLogicalPosition(pos).line)
  return this.inlayModel.getInlineElementsInRange(lineStartNewOffset, newOffset).size
}

fun VisualPosition.toInlayAwareOffset(caret: Caret): Int =this.column - caret.amountOfInlaysBeforeCaret
