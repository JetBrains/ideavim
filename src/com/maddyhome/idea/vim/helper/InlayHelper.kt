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
 * Caret.moveToOffset tries to position the caret between an inlay and the text it relates to. An inlay is added at an
 * offset, which can be considered as the start of a character, and is treated as a new variable width visual column.
 * This column is always inserted before the text at that offset, regardless of how the inlay relates to preceding or
 * following text. If it relates to preceding text, moveToOffset will place the caret at the visual column of the inlay,
 * after the related text and on/before the inlay. If it relates to the following text, it's placed at the visual
 * column of the text, after the inlay.
 *
 * This behaviour is fine for the bar caret, but for inlays related to preceding text, the block caret will be drawn
 * over the inlay, which is a poor experience for Vim users (e.g. hitting `x` in this location will delete the text
 * after the inlay, which is at the same offset as the inlay).
 *
 * This method replaces moveToOffset, and makes sure the block caret is not positioned over an inlay. We assume that
 * insert/replace and select modes use the bar caret and let the existing moveToOffset position the caret correctly
 * between the inlay and its related text. Otherwise, it's a block caret, so we always position it on the visual column
 * of the text, after the inlay.
 *
 * It is recommended to call this method even if the caret hasn't been moved. It will handle the situation where the
 * document has been changed to add an inlay at the caret position, and will move the caret appropriately.
 */
fun Caret.moveToInlayAwareOffset(offset: Int) {
  // If the target is inside a fold, call the standard moveToOffset to expand and move
  if (editor.foldingModel.isOffsetCollapsed(offset) || isBarCaret(this)) {
    moveToOffset(offset)
  } else {
    val newVisualPosition = getVisualPositionForTextAtOffset(editor, offset)
    if (newVisualPosition != visualPosition) {
      moveToVisualPosition(newVisualPosition)
    }
  }
}

fun Caret.moveToInlayAwareLogicalPosition(pos: LogicalPosition) {
  moveToInlayAwareOffset(editor.logicalPositionToOffset(pos))
}

private fun isBarCaret(caret: Caret): Boolean {
  // TODO: This should ideally be based on caret shape, rather than mode. We can't guarantee that insert means bar
  return caret.editor.inInsertMode || caret.editor.inSelectMode
}

private fun getVisualPositionForTextAtOffset(editor: Editor, offset: Int): VisualPosition {
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

fun VisualPosition.toInlayAwareOffset(caret: Caret): Int = this.column - caret.amountOfInlaysBeforeCaret

fun Editor.updateCaretsVisualPosition() {
  // Caret visual position depends on the current mode, especially with respect to inlays. E.g. if an inlay is
  // related to preceding text, the caret is placed between inlay and preceding text in insert mode (usually bar
  // caret) but after the inlay in normal mode (block caret).
  // By repositioning to the same offset, we will recalculate the expected visual position and put the caret in the
  // right location. Don't open a fold if the caret is inside
  this.vimForEachCaret {
    if (!this.foldingModel.isOffsetCollapsed(it.offset)) {
      it.moveToInlayAwareOffset(it.offset)
    }
  }
}
