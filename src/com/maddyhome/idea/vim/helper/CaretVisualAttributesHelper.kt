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

import com.intellij.openapi.editor.CaretVisualAttributes
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState

fun resetCaret(editor: Editor, insert: Boolean) {
  editor.settings.isBlockCursor = !insert
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
  CommandState.Mode.COMMAND, CommandState.Mode.VISUAL, CommandState.Mode.REPLACE -> resetCaret(
    editor,
    false
  )
  CommandState.Mode.SELECT, CommandState.Mode.INSERT -> resetCaret(
    editor,
    VimPlugin.getEditor().isBarCursorSettings
  )
  CommandState.Mode.CMD_LINE, CommandState.Mode.OP_PENDING -> Unit
}
