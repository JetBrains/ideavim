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
package com.maddyhome.idea.vim.action.motion.scroll

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

class MotionScrollLastScreenLinePageStartAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_IGNORE_SCROLL_JUMP)

  override fun execute(editor: Editor, context: DataContext, cmd: Command): Boolean {
    val motion = VimPlugin.getMotion()

    // Without [count]: Redraw with the line just above the window at the bottom of the window. Put the cursor in that
    // line, at the first non-blank in the line.
    if (cmd.rawCount == 0) {
      val prevVisualLine = EditorHelper.normalizeVisualLine(editor,
        EditorHelper.getVisualLineAtTopOfScreen(editor) - 1)
      val logicalLine = EditorHelper.visualLineToLogicalLine(editor, prevVisualLine)
      return motion.scrollLineToLastScreenLine(editor, logicalLine + 1, true)
    }

    // [count]z^ first scrolls [count] to the bottom of the window, then moves the caret to the line that is now at
    // the top, and then move that line to the bottom of the window
    var logicalLine = EditorHelper.normalizeLine(editor, cmd.rawCount - 1)
    if (motion.scrollLineToLastScreenLine(editor, logicalLine + 1, false)) {
      logicalLine = EditorHelper.visualLineToLogicalLine(editor, EditorHelper.getVisualLineAtTopOfScreen(editor))
      return motion.scrollLineToLastScreenLine(editor, logicalLine + 1, true)
    }

    return false
  }
}
