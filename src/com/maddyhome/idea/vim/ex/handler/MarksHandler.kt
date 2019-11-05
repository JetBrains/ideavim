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
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.ex.*
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.StringHelper.stringToKeys
import com.maddyhome.idea.vim.helper.StringHelper.toKeyNotation

class MarksHandler : CommandHandler.SingleExecution() {
  override val argFlags: CommandHandlerFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {

    val res = VimPlugin.getMark().getMarks(editor).joinToString("\n") { mark ->

      val text = StringBuilder()
      text.append(" ")
      text.append(mark.key)

      text.append("   ")
      var num = (mark.logicalLine + 1).toString()
      text.append(num.padStart(5))

      text.append("  ")
      num = (mark.col + 1).toString()
      text.append(num.padStart(3))

      text.append(" ")
      val vf = EditorHelper.getVirtualFile(editor)
      if (vf != null && vf.path == mark.filename) {
        text.append(toKeyNotation(stringToKeys(EditorHelper.getLineText(editor, mark.logicalLine).trim())))
      } else {
        text.append(mark.filename)
      }

      text.toString()
    }

    ExOutputModel.getInstance(editor).output("mark  line  col file/text\n$res")

    return true
  }
}
