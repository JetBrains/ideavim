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

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :dumpline"
 */
data class DumpLineCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)
  override fun processCommand(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    if (!logger.isDebug()) return ExecutionResult.Error

    val range = getLineRange(editor)
    val chars = editor.charsSequence()
    for (l in range.startLine..range.endLine) {
      val start = editor.getLineStartOffset(l)
      val end = editor.getLineEndOffset(l, true)

      logger.debug("Line $l, start offset=$start, end offset=$end")

      for (i in start..end) {
        logger.debug(
          "Offset $i, char=${chars[i]}, lp=${editor.offsetToLogicalPosition(i)}, vp=${editor.offsetToVisualPosition(i)}"
        )
      }
    }

    return ExecutionResult.Success
  }

  companion object {
    private val logger = vimLogger<DumpLineCommand>()
  }
}
