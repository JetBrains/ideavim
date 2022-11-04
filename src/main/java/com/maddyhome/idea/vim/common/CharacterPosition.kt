/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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

    fun atCaret(editor: Editor): CharacterPosition {
      return fromOffset(editor, editor.caretModel.offset)
    }
  }
}
