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
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext

/**
 * see "h :previous" / "h :bprevious"
 */
data class PreviousFileCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_IS_COUNT, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)
  override fun processCommand(editor: Editor, context: DataContext, vimContext: VimContext): ExecutionResult {
    val count = getCount(editor, 1, true)

    VimPlugin.getMark().saveJumpLocation(editor)
    VimPlugin.getFile().selectNextFile(-count, context)

    return ExecutionResult.Success
  }
}
