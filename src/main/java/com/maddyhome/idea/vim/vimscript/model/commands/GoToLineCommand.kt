/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.group.MotionGroup
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import java.lang.Integer.min

/**
 * see "h :[range]"
 */
data class GoToLineCommand(val ranges: Ranges) :
  Command.ForEachCaret(ranges) {

  override val argFlags = flags(RangeFlag.RANGE_REQUIRED, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: Editor,
    caret: Caret,
    context: DataContext,
  ): ExecutionResult {
    val line = min(this.getLine(editor, caret), EditorHelper.getLineCount(editor) - 1)

    if (line >= 0) {
      val offset = VimPlugin.getMotion().moveCaretToLineWithStartOfLineOption(editor.vim, line, caret.vim)
      MotionGroup.moveCaret(editor, caret, offset)
      return ExecutionResult.Success
    }

    MotionGroup.moveCaret(editor, caret, 0)
    return ExecutionResult.Error
  }
}
