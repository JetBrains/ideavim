/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.maddyhome.idea.vim.newapi.vim

public class CharacterPosition(line: Int, col: Int) : LogicalPosition(line, col) {
  public fun toOffset(editor: Editor): Int = editor.vim.getLineStartOffset(line) + column

  public companion object {
    public fun fromOffset(editor: Editor, offset: Int): CharacterPosition {
      // logical position "expands" tabs
      val logicalPosition = editor.offsetToLogicalPosition(offset)
      val lineStartOffset = editor.vim.getLineStartOffset(logicalPosition.line)
      return CharacterPosition(logicalPosition.line, offset - lineStartOffset)
    }

    public fun atCaret(editor: Editor): CharacterPosition {
      return fromOffset(editor, editor.caretModel.offset)
    }
  }
}
