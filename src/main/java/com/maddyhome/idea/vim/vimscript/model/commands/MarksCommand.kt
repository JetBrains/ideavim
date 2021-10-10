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
import com.maddyhome.idea.vim.ex.ExOutputModel
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.StringHelper.stringToKeys
import com.maddyhome.idea.vim.helper.StringHelper.toPrintableCharacters
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult

/**
 * see "h :marks"
 */
data class MarksCommand(val ranges: Ranges, val argument: String) : Command.SingleExecution(ranges, argument) {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(editor: Editor, context: DataContext): ExecutionResult {

    // Yeah, lower case. Vim uses lower case here, but Title Case in :registers. Go figure.
    val res = VimPlugin.getMark().getMarks(editor)
      .filter { argument.isEmpty() || argument.contains(it.key) }
      .joinToString("\n", prefix = "mark line  col file/text\n") { mark ->

        // Lines are 1 based, columns zero based. See :help :marks
        val line = (mark.logicalLine + 1).toString().padStart(5)
        val column = mark.col.toString().padStart(3)
        val vf = EditorHelper.getVirtualFile(editor)
        val text = if (vf != null && vf.path == mark.filename) {
          val lineText = EditorHelper.getLineText(editor, mark.logicalLine).trim().take(200)
          toPrintableCharacters(stringToKeys(lineText)).take(200)
        } else {
          mark.filename
        }

        " ${mark.key}  $line  $column $text"
      }

    ExOutputModel.getInstance(editor).output(res)

    return ExecutionResult.Success
  }
}
