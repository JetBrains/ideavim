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

package com.maddyhome.idea.vim.common

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.helper.EditorHelper.getLineStartOffset


class CharacterPosition(line: Int, col: Int) : LogicalPosition(line, col) {
  fun toOffset(editor: Editor) = getLineStartOffset(editor, line) + column

  companion object {
    fun fromOffset(editor: Editor, offset: Int): CharacterPosition {
      // logical position "expands" tabs
      val logicalPosition = editor.offsetToLogicalPosition(offset)
      val lineStartOffset = getLineStartOffset(editor, logicalPosition.line)
      return CharacterPosition(logicalPosition.line, offset - lineStartOffset)
    }
  }
}
