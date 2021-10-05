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

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :<"
 */
data class ShiftLeftCommand(val ranges: Ranges, val argument: String, val length: Int) : Command.ForEachCaret(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.WRITABLE)

  override fun processCommand(editor: Editor, caret: Caret, context: DataContext): ExecutionResult {
    val range = getTextRange(editor, caret, true)
    val endOffsets = range.endOffsets.map { it - 1 }.toIntArray()
    VimPlugin.getChange().indentRange(
      editor, caret, context,
      TextRange(range.startOffsets, endOffsets),
      length, -1
    )
    return ExecutionResult.Success
  }
}
