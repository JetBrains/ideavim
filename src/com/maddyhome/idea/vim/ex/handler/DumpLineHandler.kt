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

package com.maddyhome.idea.vim.ex.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.flags

class DumpLineHandler : CommandHandler.SingleExecution() {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)
  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    if (!logger.isDebugEnabled) return false

    val range = cmd.getLineRange(editor)
    val chars = editor.document.charsSequence
    for (l in range.startLine..range.endLine) {
      val start = editor.document.getLineStartOffset(l)
      val end = editor.document.getLineEndOffset(l)

      logger.debug("Line $l, start offset=$start, end offset=$end")

      for (i in start..end) {
        logger.debug("Offset $i, char=${chars[i]}, lp=${editor.offsetToLogicalPosition(i)}, vp=${editor.offsetToVisualPosition(i)}")
      }
    }

    return true
  }

  companion object {
    private val logger = Logger.getInstance(DumpLineHandler::class.java.name)
  }
}
