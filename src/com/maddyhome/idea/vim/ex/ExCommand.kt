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
package com.maddyhome.idea.vim.ex

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.ex.ranges.Ranges

class ExCommand(val ranges: Ranges, val command: String, var argument: String) {

  fun getLine(editor: Editor): Int = ranges.getLine(editor)

  fun getLine(editor: Editor, caret: Caret): Int = ranges.getLine(editor, caret)

  fun getCount(editor: Editor, defaultCount: Int, checkCount: Boolean): Int {
    val count = if (checkCount) countArgument else -1

    val res = ranges.getCount(editor, count)
    return if (res == -1) defaultCount else res
  }

  fun getCount(editor: Editor, caret: Caret, defaultCount: Int, checkCount: Boolean): Int {
    val count = ranges.getCount(editor, caret, if (checkCount) countArgument else -1)
    return if (count == -1) defaultCount else count
  }

  fun getLineRange(editor: Editor): LineRange = ranges.getLineRange(editor, -1)

  fun getLineRange(editor: Editor, caret: Caret): LineRange {
    return ranges.getLineRange(editor, caret, -1)
  }

  fun getTextRange(editor: Editor, checkCount: Boolean): TextRange {
    val count = if (checkCount) countArgument else -1
    return ranges.getTextRange(editor, count)
  }

  fun getTextRange(editor: Editor, caret: Caret, checkCount: Boolean): TextRange {
    return ranges.getTextRange(editor, caret, if (checkCount) countArgument else -1)
  }

  private val countArgument: Int
    get() = argument.toIntOrNull() ?: -1
}
