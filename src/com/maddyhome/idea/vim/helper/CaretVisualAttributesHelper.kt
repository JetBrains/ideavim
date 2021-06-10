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

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretVisualAttributes
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState

/**
 * Force the use of the bar caret
 *
 * Avoid this if possible - we should be using caret shape based on mode. This is only used for IntelliJ specific
 * behaviour, e.g. handling selection updates during mouse drag.
 */
fun Caret.forceBarCursor() {
  setPrimaryCaretShape(editor, false)
}

fun Editor.updateCaretsVisualAttributes() {
  updatePrimaryCaretVisualAttributes(this, mode)
  updateSecondaryCaretsVisualAttributes(this, inBlockSubMode)
}

private fun setPrimaryCaretShape(editor: Editor, isBlockCursor: Boolean) {
  editor.settings.isBlockCursor = isBlockCursor
}

private fun updatePrimaryCaretVisualAttributes(editor: Editor, mode: CommandState.Mode) {
  // Note that Vim uses the VISUAL caret for SELECT. We're matching INSERT
  when (mode) {
    CommandState.Mode.COMMAND, CommandState.Mode.VISUAL, CommandState.Mode.REPLACE -> setPrimaryCaretShape(editor, true)
    CommandState.Mode.SELECT, CommandState.Mode.INSERT -> setPrimaryCaretShape(editor, !VimPlugin.getEditor().isBarCursorSettings)
    CommandState.Mode.CMD_LINE, CommandState.Mode.OP_PENDING -> Unit
  }
}

private fun updateSecondaryCaretsVisualAttributes(editor: Editor, inBlockSubMode: Boolean) {
  val attributes = getVisualAttributesForSecondaryCarets(editor, inBlockSubMode)
  editor.caretModel.allCarets.forEach {
    if (it != editor.caretModel.primaryCaret) {
      it.visualAttributes = attributes
    }
  }
}

private fun getVisualAttributesForSecondaryCarets(editor: Editor, inBlockSubMode: Boolean) = if (inBlockSubMode) {
  // IntelliJ simulates visual block with multiple carets with selections. Do our best to hide them
  val color = editor.colorsScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR)
  CaretVisualAttributes(color, CaretVisualAttributes.Weight.NORMAL)
}
else {
  CaretVisualAttributes.DEFAULT
}
